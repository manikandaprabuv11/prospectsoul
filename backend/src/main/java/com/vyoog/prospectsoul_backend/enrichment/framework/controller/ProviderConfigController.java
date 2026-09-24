package com.vyoog.prospectsoul_backend.enrichment.framework.controller;

import java.util.List;

import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.enrichment.framework.dto.request.UpdateProviderConfigRequest;
import com.vyoog.prospectsoul_backend.enrichment.framework.dto.response.ProviderConfigResponse;
import com.vyoog.prospectsoul_backend.enrichment.framework.mapper.EnrichmentMapper;
import com.vyoog.prospectsoul_backend.enrichment.framework.service.ProviderConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/enrichment/providers")
@RequiredArgsConstructor
public class ProviderConfigController {

    private final ProviderConfigService configService;
    private final EnrichmentMapper mapper;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<ProviderConfigResponse> listProviders() {
        return configService.listAll().stream()
                .map(mapper::toConfigResponse)
                .toList();
    }

    @GetMapping("/{providerKey}")
    @PreAuthorize(RoleConstants.HAS_READ)
    public ProviderConfigResponse getProvider(@PathVariable String providerKey) {
        return mapper.toConfigResponse(configService.getByKey(providerKey));
    }

    @PatchMapping("/{providerKey}")
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public ProviderConfigResponse updateProvider(
            @PathVariable String providerKey,
            @Valid @RequestBody UpdateProviderConfigRequest request,
            Authentication auth) {
        String actor = CurrentUser.id(auth);
        return mapper.toConfigResponse(configService.update(providerKey, request, actor));
    }
}
