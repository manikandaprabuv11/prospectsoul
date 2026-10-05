package com.vyoog.prospectsoul_backend.location.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record MapDownloadRequest(
        @NotBlank @Pattern(regexp = "\\d{6}") String pincode,
        @Positive Double radiusKm,
        List<UUID> nicParentIds,
        Boolean nicIncludeDescendants,
        Boolean applyDefaults
) {
    public double effectiveRadiusKm() {
        return radiusKm != null ? radiusKm : 5.0;
    }

    public boolean effectiveApplyDefaults() {
        return applyDefaults == null || applyDefaults;
    }

    public boolean effectiveNicIncludeDescendants() {
        return nicIncludeDescendants == null || nicIncludeDescendants;
    }
}
