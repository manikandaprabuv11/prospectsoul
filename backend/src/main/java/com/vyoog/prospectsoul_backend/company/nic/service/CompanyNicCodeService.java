package com.vyoog.prospectsoul_backend.company.nic.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.common.exception.ConflictException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.nic.dto.request.AttachNicCodeRequest;
import com.vyoog.prospectsoul_backend.company.nic.dto.response.CompanyNicCodeResponse;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import com.vyoog.prospectsoul_backend.company.nic.mapper.CompanyNicCodeMapper;
import com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Multi-NIC per company (ADR-0002). Enforces Domain Model Addendum
 * Invariant 12: exactly one primary NIC per company, with the denormalised
 * {@code companies.primary_nic_code_id} always in sync.
 */
@Service
@RequiredArgsConstructor
public class CompanyNicCodeService {

    private final CompanyNicCodeRepository joinRepository;
    private final CompanyNicCodeMapper mapper;
    private final CompanyRepository companyRepository;
    private final NicCodeRepository nicCodeRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CompanyNicCodeResponse> list(UUID companyId) {
        assertCompanyExists(companyId);
        return joinRepository.findByCompanyIdOrderBySequenceNoAsc(companyId).stream()
                .map(mapper::toResponse).toList();
    }

    @Transactional
    public CompanyNicCodeResponse attach(UUID companyId, AttachNicCodeRequest req, String actor) {
        assertCompanyExists(companyId);
        NicCode resolved = null;
        String raw;
        String descRaw = req.descriptionRaw();
        if (req.nicCodeId() != null) {
            resolved = nicCodeRepository.findById(req.nicCodeId())
                    .orElseThrow(() -> new BusinessRuleException("NIC code not found: " + req.nicCodeId()));
            raw = resolved.getCode();
            if (descRaw == null || descRaw.isBlank()) descRaw = resolved.getDescription();
        } else if (req.code() != null && !req.code().isBlank()) {
            raw = req.code().trim();
            resolved = nicCodeRepository.findByCode(raw).orElse(null);
            if (descRaw == null || descRaw.isBlank())
                descRaw = resolved != null ? resolved.getDescription() : null;
        } else {
            throw new BusinessRuleException("attach requires nic_code_id or code");
        }

        if (joinRepository.findByCompanyIdAndNicCodeRaw(companyId, raw).isPresent()) {
            throw new ConflictException("NIC code " + raw + " already attached to this company");
        }

        short next = (short) (joinRepository.countByCompanyId(companyId) + 1);
        boolean primaryRequested = Boolean.TRUE.equals(req.isPrimary()) || next == 1;

        CompanyNicCode row = joinRepository.save(CompanyNicCode.builder()
                .companyId(companyId)
                .nicCode(resolved)
                .nicCodeRaw(raw)
                .descriptionRaw(descRaw)
                .sequenceNo(next)
                .isPrimary(false)
                .build());

        if (primaryRequested) {
            makePrimaryInternal(companyId, row);
        }

        auditService.record("COMPANY_NIC_CODE", row.getId(), actor, "ATTACH",
                null, mapper.toResponse(row));
        return mapper.toResponse(row);
    }

    @Transactional
    public CompanyNicCodeResponse makePrimary(UUID companyId, UUID rowId, String actor) {
        CompanyNicCode row = joinRepository.findById(rowId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyNicCode", rowId));
        if (!row.getCompanyId().equals(companyId)) {
            throw new BusinessRuleException("Row does not belong to the given company");
        }
        var before = mapper.toResponse(row);
        makePrimaryInternal(companyId, row);
        auditService.record("COMPANY_NIC_CODE", row.getId(), actor, "MAKE_PRIMARY",
                before, mapper.toResponse(row));
        return mapper.toResponse(row);
    }

    @Transactional
    public void detach(UUID companyId, UUID rowId, String actor) {
        CompanyNicCode row = joinRepository.findById(rowId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyNicCode", rowId));
        if (!row.getCompanyId().equals(companyId)) {
            throw new BusinessRuleException("Row does not belong to the given company");
        }
        var before = mapper.toResponse(row);
        boolean wasPrimary = Boolean.TRUE.equals(row.getIsPrimary());
        joinRepository.delete(row);
        joinRepository.flush();

        if (wasPrimary) {
            // Promote the earliest surviving row (if any) to keep the denorm consistent.
            Optional<CompanyNicCode> next = joinRepository
                    .findByCompanyIdOrderBySequenceNoAsc(companyId).stream().findFirst();
            if (next.isPresent()) {
                makePrimaryInternal(companyId, next.get());
            } else {
                Company c = companyRepository.findById(companyId).orElse(null);
                if (c != null) {
                    c.setPrimaryNicCodeId(null);
                    companyRepository.save(c);
                }
            }
        }

        auditService.record("COMPANY_NIC_CODE", rowId, actor, "DETACH", before, null);
    }

    private void makePrimaryInternal(UUID companyId, CompanyNicCode target) {
        joinRepository.demoteOthers(companyId, target.getId());
        target.setIsPrimary(true);
        joinRepository.saveAndFlush(target);
        // Keep the denormalised pointer in sync — may be NULL when the raw
        // code did not match the master (Domain Model Addendum §5).
        Company c = companyRepository.findById(companyId).orElse(null);
        if (c != null) {
            c.setPrimaryNicCodeId(target.getNicCodeId());
            companyRepository.save(c);
        }
    }

    private void assertCompanyExists(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company", companyId);
        }
    }
}
