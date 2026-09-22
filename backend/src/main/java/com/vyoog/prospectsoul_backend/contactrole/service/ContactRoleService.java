package com.vyoog.prospectsoul_backend.contactrole.service;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.exception.ConflictException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.contactrole.dto.request.ContactRoleCreateRequest;
import com.vyoog.prospectsoul_backend.contactrole.dto.request.ContactRoleUpdateRequest;
import com.vyoog.prospectsoul_backend.contactrole.dto.response.ContactRoleResponse;
import com.vyoog.prospectsoul_backend.contactrole.entity.ContactRole;
import com.vyoog.prospectsoul_backend.contactrole.mapper.ContactRoleMapper;
import com.vyoog.prospectsoul_backend.contactrole.repository.ContactRoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactRoleService {

    private final ContactRoleRepository contactRoleRepository;
    private final ContactRoleMapper contactRoleMapper;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<ContactRoleResponse> list(boolean includeInactive) {
        List<ContactRole> rows = includeInactive
                ? contactRoleRepository.findAllByOrderBySortOrderAscLabelAsc()
                : contactRoleRepository.findByActiveTrueOrderBySortOrderAscLabelAsc();
        return rows.stream().map(r -> contactRoleMapper.toResponse(r, usageCount(r.getId()))).toList();
    }

    @Transactional
    public ContactRoleResponse create(ContactRoleCreateRequest req, String actor) {
        String key = req.key().trim();
        if (contactRoleRepository.findByKey(key).isPresent()) {
            throw new ConflictException("Contact role key already exists: " + key);
        }
        ContactRole saved = contactRoleRepository.save(ContactRole.builder()
                .key(key)
                .label(req.label().trim())
                .sortOrder(req.sortOrder() == null ? (short) 100 : req.sortOrder())
                .active(true)
                .build());
        auditService.record("CONTACT_ROLE", saved.getId(), actor, "CREATE", null,
                contactRoleMapper.toResponse(saved, 0));
        return contactRoleMapper.toResponse(saved, 0);
    }

    @Transactional
    public ContactRoleResponse update(UUID id, ContactRoleUpdateRequest req, String actor) {
        ContactRole r = contactRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactRole", id));
        var before = contactRoleMapper.toResponse(r, usageCount(id));
        if (req.label() != null) r.setLabel(req.label().trim());
        if (req.sortOrder() != null) r.setSortOrder(req.sortOrder());
        if (req.active() != null) r.setActive(req.active());
        ContactRole saved = contactRoleRepository.save(r);
        auditService.record("CONTACT_ROLE", saved.getId(), actor, "UPDATE", before,
                contactRoleMapper.toResponse(saved, usageCount(id)));
        return contactRoleMapper.toResponse(saved, usageCount(id));
    }

    long usageCount(UUID roleId) {
        try {
            Object result = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM contacts WHERE role_id = :id")
                    .setParameter("id", roleId)
                    .getSingleResult();
            return result == null ? 0 : ((Number) result).longValue();
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
