package com.vyoog.prospectsoul_backend.contact.service;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.contact.dto.request.ContactCreateRequest;
import com.vyoog.prospectsoul_backend.contact.dto.request.ContactUpdateRequest;
import com.vyoog.prospectsoul_backend.contact.dto.response.ContactResponse;
import com.vyoog.prospectsoul_backend.contact.entity.Contact;
import com.vyoog.prospectsoul_backend.contact.mapper.ContactMapper;
import com.vyoog.prospectsoul_backend.contact.repository.ContactRepository;
import com.vyoog.prospectsoul_backend.contactrole.entity.ContactRole;
import com.vyoog.prospectsoul_backend.contactrole.repository.ContactRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Multi-contact-per-company service (ADR-0001, ADR-0006). Enforces:
 *   - role_id is required on create (Domain Model Addendum Invariant 14),
 *   - at most one primary contact per company — atomic swap (Invariant 13),
 *   - is_md_owner stays in sync with role_key == "MD_OWNER".
 */
@Service
@RequiredArgsConstructor
public class ContactService {

    private static final String MD_OWNER_KEY = "MD_OWNER";

    private final ContactRepository contactRepository;
    private final ContactRoleRepository contactRoleRepository;
    private final CompanyRepository companyRepository;
    private final ContactMapper contactMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ContactResponse> listForCompany(UUID companyId) {
        assertCompanyExists(companyId);
        return contactRepository.findByCompanyIdOrderByIsPrimaryDescCreatedAtAsc(companyId).stream()
                .map(contactMapper::toResponse).toList();
    }

    @Transactional
    public ContactResponse create(UUID companyId, ContactCreateRequest req, String actor) {
        assertCompanyExists(companyId);
        ContactRole role = contactRoleRepository.findById(req.roleId())
                .orElseThrow(() -> new BusinessRuleException("Contact role not found: " + req.roleId()));
        if (!Boolean.TRUE.equals(role.getActive())) {
            throw new BusinessRuleException("Contact role is inactive: " + role.getKey());
        }

        boolean asPrimary = Boolean.TRUE.equals(req.isPrimary());
        // If this is the first contact, promote it to primary regardless.
        if (!asPrimary && contactRepository.countByCompanyId(companyId) == 0) {
            asPrimary = true;
        }

        Contact c = Contact.builder()
                .companyId(companyId)
                .name(req.name().trim())
                .designation(trimOrNull(req.designation()))
                .phone(trimOrNull(req.phone()))
                .email(trimOrNull(req.email()))
                .role(role)
                .isPrimary(false)  // set below after atomic swap
                .isMdOwner(MD_OWNER_KEY.equals(role.getKey()))
                .associationStart(req.associationStart())
                .associationEnd(req.associationEnd())
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        Contact saved = contactRepository.save(c);
        if (asPrimary) makePrimaryInternal(companyId, saved);

        auditService.record("CONTACT", saved.getId(), actor, "CREATE",
                null, contactMapper.toResponse(saved));
        return contactMapper.toResponse(saved);
    }

    @Transactional
    public ContactResponse update(UUID contactId, ContactUpdateRequest req, String actor) {
        Contact c = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", contactId));
        var before = contactMapper.toResponse(c);

        if (req.name() != null) c.setName(req.name().trim());
        if (req.designation() != null) c.setDesignation(trimOrNull(req.designation()));
        if (req.phone() != null) c.setPhone(trimOrNull(req.phone()));
        if (req.email() != null) c.setEmail(trimOrNull(req.email()));
        if (req.associationStart() != null) c.setAssociationStart(req.associationStart());
        if (req.associationEnd() != null) c.setAssociationEnd(req.associationEnd());

        if (req.roleId() != null) {
            ContactRole role = contactRoleRepository.findById(req.roleId())
                    .orElseThrow(() -> new BusinessRuleException("Contact role not found: " + req.roleId()));
            c.setRole(role);
            c.setIsMdOwner(MD_OWNER_KEY.equals(role.getKey()));
        }

        c.setUpdatedBy(actor);
        Contact saved = contactRepository.save(c);

        if (Boolean.TRUE.equals(req.isPrimary()) && !Boolean.TRUE.equals(before.isPrimary())) {
            makePrimaryInternal(c.getCompanyId(), saved);
        }

        auditService.record("CONTACT", saved.getId(), actor, "UPDATE", before,
                contactMapper.toResponse(saved));
        return contactMapper.toResponse(saved);
    }

    @Transactional
    public ContactResponse makePrimary(UUID contactId, String actor) {
        Contact c = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", contactId));
        var before = contactMapper.toResponse(c);
        makePrimaryInternal(c.getCompanyId(), c);
        auditService.record("CONTACT", c.getId(), actor, "MAKE_PRIMARY", before,
                contactMapper.toResponse(c));
        return contactMapper.toResponse(c);
    }

    /**
     * Domain Model Addendum Invariant 13: exactly one primary per company.
     * Demote everyone else first (bulk update, no per-row round-trip), then
     * flush and mark the target row primary. The partial unique index on
     * contacts.is_primary is the DB-level backstop.
     */
    private void makePrimaryInternal(UUID companyId, Contact target) {
        contactRepository.demoteOtherPrimaries(companyId, target.getId());
        target.setIsPrimary(true);
        contactRepository.saveAndFlush(target);
    }

    private void assertCompanyExists(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company", companyId);
        }
    }

    private String trimOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
