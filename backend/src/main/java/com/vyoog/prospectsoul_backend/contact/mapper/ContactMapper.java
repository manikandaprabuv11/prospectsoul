package com.vyoog.prospectsoul_backend.contact.mapper;

import com.vyoog.prospectsoul_backend.contact.dto.response.ContactResponse;
import com.vyoog.prospectsoul_backend.contact.entity.Contact;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {
    public ContactResponse toResponse(Contact e) {
        return new ContactResponse(
                e.getId(),
                e.getCompanyId(),
                e.getName(),
                e.getDesignation(),
                e.getPhone(),
                e.getEmail(),
                e.getRoleId(),
                e.getRole() == null ? null : e.getRole().getKey(),
                e.getRole() == null ? null : e.getRole().getLabel(),
                e.getIsPrimary(),
                e.getIsMdOwner(),
                e.getAssociationStart(),
                e.getAssociationEnd(),
                e.getVerificationStatus(),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedBy(),
                e.getUpdatedAt()
        );
    }
}
