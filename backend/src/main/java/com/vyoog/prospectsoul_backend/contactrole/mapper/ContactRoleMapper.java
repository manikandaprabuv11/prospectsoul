package com.vyoog.prospectsoul_backend.contactrole.mapper;

import com.vyoog.prospectsoul_backend.contactrole.dto.response.ContactRoleResponse;
import com.vyoog.prospectsoul_backend.contactrole.entity.ContactRole;
import org.springframework.stereotype.Component;

@Component
public class ContactRoleMapper {
    public ContactRoleResponse toResponse(ContactRole e, long usageCount) {
        return new ContactRoleResponse(
                e.getId(), e.getKey(), e.getLabel(), e.getSortOrder(),
                e.getActive(), usageCount, e.getCreatedAt(), e.getUpdatedAt());
    }
}
