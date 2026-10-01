package com.vyoog.prospectsoul_backend.company.phone.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.common.exception.ConflictException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.phone.dto.request.CompanyPhoneRequest;
import com.vyoog.prospectsoul_backend.company.phone.dto.request.ConfidenceOverrideRequest;
import com.vyoog.prospectsoul_backend.company.phone.dto.response.CompanyPhoneResponse;
import com.vyoog.prospectsoul_backend.company.phone.entity.CompanyPhone;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceMode;
import com.vyoog.prospectsoul_backend.company.phone.entity.NumberSourceType;
import com.vyoog.prospectsoul_backend.company.phone.mapper.CompanyPhoneMapper;
import com.vyoog.prospectsoul_backend.company.phone.repository.CompanyPhoneRepository;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.contact.entity.Contact;
import com.vyoog.prospectsoul_backend.contact.repository.ContactRepository;
import com.vyoog.prospectsoul_backend.imports.normalization.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyPhoneService {

    private final CompanyPhoneRepository phoneRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final CompanyPhoneMapper phoneMapper;
    private final ConfidenceEngine confidenceEngine;
    private final PhoneNormalizer phoneNormalizer;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CompanyPhoneResponse> listForCompany(UUID companyId) {
        return phoneRepository.findByCompanyIdOrderByIsPrimaryDescConfidenceAscCreatedAtDesc(companyId)
                .stream()
                .map(phoneMapper::toResponse)
                .toList();
    }

    @Transactional
    public CompanyPhoneResponse addPhone(UUID companyId, CompanyPhoneRequest request, String actor) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));

        String normalized = phoneNormalizer.normalize(request.numberRaw());
        PhoneNormalizationResult normResult = PhoneNormalizationResult.classify(normalized);

        // Dedup check: same phone on same company
        if (normResult.valid()) {
            phoneRepository.findByCompanyIdAndNumberNormalized(companyId, normResult.normalized())
                    .ifPresent(existing -> {
                        throw new ConflictException("This phone number already exists on this company");
                    });

            // Dedup check: same phone on different company
            List<CompanyPhone> matches = phoneRepository.findByNumberNormalized(normResult.normalized());
            for (CompanyPhone match : matches) {
                if (!match.getCompany().getId().equals(companyId)) {
                    throw new ConflictException(
                            "This phone number exists on company " + match.getCompany().getCanonicalName(),
                            Map.of("match", Map.of(
                                    "company_id", match.getCompany().getId(),
                                    "company_name", match.getCompany().getCanonicalName(),
                                    "phone_id", match.getId()
                            ))
                    );
                }
            }
        }

        Contact contact = resolveContact(request.contactId(), companyId);
        String designation = resolveDesignation(contact, request.designationOverride());

        ConfidenceEngine.ComputeResult confidenceResult = confidenceEngine.compute(
                request.numberSource(), designation, null, null, request.confidence());

        CompanyPhone phone = CompanyPhone.builder()
                .company(company)
                .contact(contact)
                .numberRaw(request.numberRaw())
                .numberNormalized(normResult.valid() ? normResult.normalized() : null)
                .phoneType(normResult.phoneType())
                .numberSource(request.numberSource())
                .confidence(confidenceResult.confidence())
                .confidenceMode(confidenceResult.mode())
                .designationOverride(request.designationOverride())
                .isPrimary(Boolean.TRUE.equals(request.isPrimary()))
                .createdBy(parseUuid(actor))
                .updatedBy(parseUuid(actor))
                .build();

        if (Boolean.TRUE.equals(request.isPrimary())) {
            phoneRepository.clearAllPrimaries(companyId);
        }

        CompanyPhone saved = phoneRepository.save(phone);
        recomputePrimary(companyId);

        auditService.record("COMPANY_PHONE", saved.getId(), actor, "PHONE_ADDED",
                null, phoneMapper.toResponse(saved));

        return phoneMapper.toResponse(saved);
    }

    @Transactional
    public CompanyPhoneResponse updatePhone(UUID phoneId, CompanyPhoneRequest request, String actor) {
        CompanyPhone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyPhone", phoneId));

        CompanyPhoneResponse previousState = phoneMapper.toResponse(phone);
        UUID companyId = phone.getCompany().getId();

        if (request.numberRaw() != null && !request.numberRaw().equals(phone.getNumberRaw())) {
            String normalized = phoneNormalizer.normalize(request.numberRaw());
            PhoneNormalizationResult normResult = PhoneNormalizationResult.classify(normalized);
            phone.setNumberRaw(request.numberRaw());
            phone.setNumberNormalized(normResult.valid() ? normResult.normalized() : null);
            phone.setPhoneType(normResult.phoneType());
        }

        if (request.numberSource() != null) {
            phone.setNumberSource(request.numberSource());
        }

        if (request.contactId() != null) {
            Contact contact = resolveContact(request.contactId(), companyId);
            phone.setContact(contact);
        }

        if (request.designationOverride() != null) {
            phone.setDesignationOverride(request.designationOverride());
        }

        if (Boolean.TRUE.equals(request.isPrimary()) && !Boolean.TRUE.equals(phone.getIsPrimary())) {
            phoneRepository.clearOtherPrimaries(companyId, phoneId);
            phone.setIsPrimary(true);
        }

        // Recompute confidence if source or contact changed
        if (phone.getConfidenceMode() != ConfidenceMode.MANUAL) {
            String designation = phone.resolveDesignation();
            ConfidenceEngine.ComputeResult result = confidenceEngine.compute(
                    phone.getNumberSource(), designation,
                    phone.getConfidence(), phone.getConfidenceMode(),
                    request.confidence());
            phone.setConfidence(result.confidence());
            phone.setConfidenceMode(result.mode());
        }

        phone.setUpdatedBy(parseUuid(actor));
        CompanyPhone saved = phoneRepository.save(phone);
        recomputePrimary(companyId);

        auditService.record("COMPANY_PHONE", saved.getId(), actor, "PHONE_UPDATED",
                previousState, phoneMapper.toResponse(saved));

        return phoneMapper.toResponse(saved);
    }

    @Transactional
    public void removePhone(UUID phoneId, String actor) {
        CompanyPhone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyPhone", phoneId));

        CompanyPhoneResponse previousState = phoneMapper.toResponse(phone);
        UUID companyId = phone.getCompany().getId();

        phoneRepository.delete(phone);

        auditService.record("COMPANY_PHONE", phoneId, actor, "PHONE_REMOVED",
                previousState, null);

        recomputePrimary(companyId);
    }

    @Transactional
    public CompanyPhoneResponse overrideConfidence(UUID phoneId, ConfidenceOverrideRequest request, String actor) {
        CompanyPhone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyPhone", phoneId));

        CompanyPhoneResponse previousState = phoneMapper.toResponse(phone);

        phone.setConfidence(request.confidence());
        phone.setConfidenceMode(ConfidenceMode.MANUAL);
        phone.setOverrideReason(request.reason());
        phone.setUpdatedBy(parseUuid(actor));

        CompanyPhone saved = phoneRepository.save(phone);
        recomputePrimary(phone.getCompany().getId());

        auditService.record("COMPANY_PHONE", saved.getId(), actor, "CONFIDENCE_OVERRIDE",
                previousState, phoneMapper.toResponse(saved));

        return phoneMapper.toResponse(saved);
    }

    @Transactional
    public void syncPhonesForCompany(UUID companyId, List<CompanyPhoneRequest> phones, String actor) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));

        List<CompanyPhone> existing = phoneRepository
                .findByCompanyIdOrderByIsPrimaryDescConfidenceAscCreatedAtDesc(companyId);
        var existingById = new java.util.HashMap<UUID, CompanyPhone>();
        existing.forEach(p -> existingById.put(p.getId(), p));

        var requestIds = new java.util.HashSet<UUID>();
        if (phones != null) {
            for (CompanyPhoneRequest req : phones) {
                if (req.id() != null) {
                    requestIds.add(req.id());
                    if (existingById.containsKey(req.id())) {
                        updatePhone(req.id(), req, actor);
                    }
                } else {
                    addPhone(companyId, req, actor);
                }
            }
        }

        // Remove phones not in the request list
        for (CompanyPhone existingPhone : existing) {
            if (!requestIds.contains(existingPhone.getId())) {
                removePhone(existingPhone.getId(), actor);
            }
        }

        recomputePrimary(companyId);
    }

    @Transactional
    public void recomputePrimary(UUID companyId) {
        List<CompanyPhone> phones = phoneRepository
                .findByCompanyIdOrderByIsPrimaryDescConfidenceAscCreatedAtDesc(companyId);

        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) return;

        if (phones.isEmpty()) {
            company.setPrimaryPhoneNormalized(null);
            companyRepository.save(company);
            return;
        }

        // Find existing primary or select the best
        CompanyPhone primary = phones.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPrimary()))
                .findFirst()
                .orElse(null);

        if (primary == null) {
            // Auto-select best: highest confidence -> decision-maker -> most recent
            primary = phones.getFirst();
            primary.setIsPrimary(true);
            phoneRepository.save(primary);
        }

        company.setPrimaryPhoneNormalized(primary.getNumberNormalized());
        companyRepository.save(company);
    }

    public void recomputeConfidenceForContact(UUID contactId) {
        List<CompanyPhone> phones = phoneRepository.findByContactId(contactId);
        for (CompanyPhone phone : phones) {
            if (phone.getConfidenceMode() == ConfidenceMode.MANUAL) continue;
            String designation = phone.resolveDesignation();
            ConfidenceEngine.ComputeResult result = confidenceEngine.compute(
                    phone.getNumberSource(), designation,
                    phone.getConfidence(), phone.getConfidenceMode(), null);
            if (result.confidence() != phone.getConfidence()) {
                phone.setConfidence(result.confidence());
                phoneRepository.save(phone);
                recomputePrimary(phone.getCompany().getId());
            }
        }
    }

    private Contact resolveContact(UUID contactId, UUID companyId) {
        if (contactId == null) return null;
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", contactId));
        if (!contact.getCompanyId().equals(companyId)) {
            throw new BusinessRuleException("Contact does not belong to this company");
        }
        return contact;
    }

    private String resolveDesignation(Contact contact, String designationOverride) {
        if (contact != null && contact.getDesignation() != null && !contact.getDesignation().isBlank()) {
            return contact.getDesignation();
        }
        return designationOverride;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
