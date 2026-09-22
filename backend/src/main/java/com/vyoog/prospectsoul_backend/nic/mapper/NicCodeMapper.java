package com.vyoog.prospectsoul_backend.nic.mapper;

import java.util.List;

import com.vyoog.prospectsoul_backend.nic.dto.response.NicCodeResponse;
import com.vyoog.prospectsoul_backend.nic.dto.response.NicTreeNodeResponse;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import org.springframework.stereotype.Component;

@Component
public class NicCodeMapper {

    public NicCodeResponse toResponse(NicCode e) {
        return new NicCodeResponse(
                e.getId(),
                e.getNicDataId(),
                e.getCode(),
                e.getDescription(),
                e.getIndustryType(),
                e.getLevel(),
                e.getParentId(),
                e.getIsPrimary(),
                e.getActive(),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedBy(),
                e.getUpdatedAt()
        );
    }

    public NicTreeNodeResponse toTreeNode(NicCode e, long childCount, List<NicTreeNodeResponse> children) {
        return new NicTreeNodeResponse(
                e.getId(),
                e.getCode(),
                e.getDescription(),
                e.getIndustryType(),
                e.getLevel(),
                e.getIsPrimary(),
                e.getActive(),
                childCount,
                children
        );
    }
}
