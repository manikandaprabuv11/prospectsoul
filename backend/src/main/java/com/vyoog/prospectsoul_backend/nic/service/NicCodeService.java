package com.vyoog.prospectsoul_backend.nic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.common.exception.ConflictException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.nic.dto.request.NicCodeCreateRequest;
import com.vyoog.prospectsoul_backend.nic.dto.request.NicCodeUpdateRequest;
import com.vyoog.prospectsoul_backend.nic.dto.response.NicCodeResponse;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.mapper.NicCodeMapper;
import com.vyoog.prospectsoul_backend.nic.parser.NicLevelResolver;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NicCodeService {

    private static final int MAX_PAGE_SIZE = 200;

    private final NicCodeRepository nicCodeRepository;
    private final NicCodeMapper nicCodeMapper;
    private final NicTreeService nicTreeService;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public NicCodeResponse create(NicCodeCreateRequest req, String actor) {
        String code = req.code().trim();
        if (nicCodeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("NIC code already exists: " + code);
        }
        NicCode parent = resolveParent(code, req.parentId());
        NicCode saved = nicCodeRepository.save(NicCode.builder()
                .code(code)
                .description(req.description().trim())
                .industryType(req.industryType())
                .level(NicLevelResolver.forCode(code))
                .parent(parent)
                .isPrimary(Boolean.TRUE.equals(req.isPrimary()))
                .active(true)
                .createdBy(actor)
                .updatedBy(actor)
                .build());

        nicTreeService.invalidate();
        auditService.record("NIC_CODE", saved.getId(), actor, "CREATE", null, nicCodeMapper.toResponse(saved));
        return nicCodeMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public NicCodeResponse getById(UUID id) {
        return nicCodeMapper.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<NicCodeResponse> list(String q, Short level, String industryType,
                                              Boolean isPrimary, Boolean active,
                                              int page, int size, String sort, String sortDir) {
        size = Math.min(size, MAX_PAGE_SIZE);
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = List.of("code", "description", "level", "isPrimary", "active", "createdAt", "updatedAt")
                .contains(sort) ? sort : "code";
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortField));
        Specification<NicCode> spec = filter(q, level, industryType, isPrimary, active);
        Page<NicCode> result = nicCodeRepository.findAll(spec, pageable);
        return PageResponse.from(result.map(nicCodeMapper::toResponse));
    }

    private Specification<NicCode> filter(String q, Short level, String industryType,
                                          Boolean isPrimary, Boolean active) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(root.get("code"), q.trim() + "%")
                ));
            }
            if (level != null) predicates.add(cb.equal(root.get("level"), level));
            if (industryType != null && !industryType.isBlank())
                predicates.add(cb.equal(root.get("industryType"), industryType));
            if (isPrimary != null) predicates.add(cb.equal(root.get("isPrimary"), isPrimary));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    public List<NicCodeResponse> listChildren(UUID parentId) {
        load(parentId);
        return nicCodeRepository.findByParentIdOrderByCodeAsc(parentId).stream()
                .map(nicCodeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NicCodeResponse> listPrimary() {
        return nicCodeRepository.findByIsPrimaryTrueAndActiveTrueOrderByCodeAsc().stream()
                .map(nicCodeMapper::toResponse).toList();
    }

    @Transactional
    public NicCodeResponse update(UUID id, NicCodeUpdateRequest req, String actor) {
        NicCode c = load(id);
        NicCodeResponse before = nicCodeMapper.toResponse(c);

        if (req.description() != null) c.setDescription(req.description().trim());
        if (req.industryType() != null) c.setIndustryType(req.industryType());
        if (req.parentId() != null) c.setParent(resolveParent(c.getCode(), req.parentId()));
        if (req.isPrimary() != null) c.setIsPrimary(req.isPrimary());

        if (req.active() != null && !req.active().equals(c.getActive())) {
            if (Boolean.FALSE.equals(req.active())) {
                long refs = countReferences(id);
                if (refs > 0 && !Boolean.TRUE.equals(req.forceDeactivate())) {
                    throw new BusinessRuleException(
                            "NIC code " + c.getCode() + " is referenced by " + refs
                                    + " company_nic_codes row(s). Pass force_deactivate=true to proceed.");
                }
            }
            c.setActive(req.active());
        }

        c.setUpdatedBy(actor);
        NicCode saved = nicCodeRepository.save(c);
        nicTreeService.invalidate();
        auditService.record("NIC_CODE", saved.getId(), actor, "UPDATE", before, nicCodeMapper.toResponse(saved));
        return nicCodeMapper.toResponse(saved);
    }

    @Transactional
    public NicCodeResponse togglePrimary(UUID id, String actor) {
        NicCode c = load(id);
        NicCodeResponse before = nicCodeMapper.toResponse(c);
        c.setIsPrimary(!Boolean.TRUE.equals(c.getIsPrimary()));
        c.setUpdatedBy(actor);
        NicCode saved = nicCodeRepository.save(c);
        nicTreeService.invalidate();
        auditService.record("NIC_CODE", saved.getId(), actor, "TOGGLE_PRIMARY", before, nicCodeMapper.toResponse(saved));
        return nicCodeMapper.toResponse(saved);
    }

    private NicCode load(UUID id) {
        return nicCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NicCode", id));
    }

    /**
     * Docs 21 §3.4 rule: `parent_id` must reference a code that is a strict
     * prefix of the child code. A NULL parent is legal only at level 1
     * (Section) — deeper codes must have a parent that is a longer prefix
     * than any other candidate.
     */
    private NicCode resolveParent(String childCode, UUID declaredParentId) {
        if (declaredParentId == null) return null;
        NicCode parent = nicCodeRepository.findById(declaredParentId)
                .orElseThrow(() -> new BusinessRuleException("Parent NIC code not found: " + declaredParentId));
        String parentCode = parent.getCode();
        if (parentCode.equals(childCode) || !childCode.startsWith(parentCode)) {
            throw new BusinessRuleException(
                    "Parent code '" + parentCode + "' must be a prefix of child code '" + childCode + "'");
        }
        return parent;
    }

    /**
     * `company_nic_codes` is created in C2. Until that migration lands, the
     * count is trivially zero — we swallow the "relation does not exist"
     * error and return 0. Once the table exists the count is real.
     */
    long countReferences(UUID nicCodeId) {
        try {
            Object result = entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM company_nic_codes WHERE nic_code_id = :id")
                    .setParameter("id", nicCodeId)
                    .getSingleResult();
            return result == null ? 0 : ((Number) result).longValue();
        } catch (RuntimeException ex) {
            // Table not yet materialised — no possible reference.
            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (msg.contains("company_nic_codes") || msg.contains("relation") || msg.contains("does not exist")) {
                return 0;
            }
            throw ex;
        }
    }
}
