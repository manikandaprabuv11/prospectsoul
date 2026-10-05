package com.vyoog.prospectsoul_backend.admin.designation.dto.request;

import java.util.List;

public record DesignationUpdateRequest(
        String designation,
        List<String> aliases,
        Boolean active
) {}
