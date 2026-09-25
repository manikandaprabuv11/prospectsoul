package com.vyoog.prospectsoul_backend.location.controller;

import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.company.defaultfilter.entity.CompanyDefaultFilter;
import com.vyoog.prospectsoul_backend.company.defaultfilter.service.CompanyDefaultFilterService;
import com.vyoog.prospectsoul_backend.location.dto.response.MapCompanyResponse;
import com.vyoog.prospectsoul_backend.location.dto.response.PincodeCentroidResponse;
import com.vyoog.prospectsoul_backend.location.service.CompanyMapService;
import com.vyoog.prospectsoul_backend.location.service.PincodeCentroidService;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class MapController {

    private final PincodeCentroidService pincodeCentroidService;
    private final CompanyMapService companyMapService;
    private final CompanyDefaultFilterService companyDefaultFilterService;
    private final NicCodeRepository nicCodeRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/pincode/{pincode}")
    @PreAuthorize(RoleConstants.HAS_READ)
    public PincodeCentroidResponse pincode(@PathVariable String pincode) {
        return pincodeCentroidService.lookup(pincode);
    }

    @GetMapping("/companies")
    @PreAuthorize(RoleConstants.HAS_READ)
    public MapCompanyResponse companies(
            @RequestParam String pincode,
            @RequestParam(name = "radius_km", defaultValue = "5") double radiusKm,
            @RequestParam(name = "nic_parent_id", required = false) UUID nicParentId,
            // Bound as List<String> and parsed manually below — same reason as
            // CompanyController: Spring's default binder silently drops
            // List<UUID> from repeated query params
            // (`?nic_parent_ids=<uuid>&nic_parent_ids=<uuid>`), so the NIC
            // predicate would be skipped and every company returned. A bad
            // token now fails loudly with 422 (BusinessRuleException) exactly
            // like the singular nic_parent_id / Companies list.
            @RequestParam(name = "nic_parent_ids", required = false) List<String> nicParentIdsRaw,
            @RequestParam(name = "nic_include_descendants", required = false) Boolean nicIncludeDescendants,
            @RequestParam(name = "apply_defaults", defaultValue = "false") boolean applyDefaults) {

        // Page-level NIC selection: the preferred multi-select `nic_parent_ids`
        // plus the singular `nic_parent_id` kept as a backwards-compatible
        // alias — both feed the same set. OR across every selected NIC, each
        // expanded to its own descendant subtree when
        // `nic_include_descendants` isn't explicitly false. Same pattern as
        // CompanyController#list.
        LinkedHashSet<UUID> pageParentIds = new LinkedHashSet<>();
        if (nicParentIdsRaw != null) {
            for (String raw : nicParentIdsRaw) {
                if (raw == null || raw.isBlank()) continue;
                // A single param value may also arrive as CSV
                // (?nic_parent_ids=a,b). Handle both shapes.
                for (String token : raw.split(",")) {
                    String t = token.trim();
                    if (t.isEmpty()) continue;
                    try {
                        pageParentIds.add(UUID.fromString(t));
                    } catch (IllegalArgumentException e) {
                        throw new BusinessRuleException("Invalid UUID in nic_parent_ids: " + t);
                    }
                }
            }
        }
        if (nicParentId != null) pageParentIds.add(nicParentId);
        boolean includeDesc = nicIncludeDescendants == null || nicIncludeDescendants;

        LinkedHashSet<UUID> pageNicIds = new LinkedHashSet<>();
        for (UUID pid : pageParentIds) {
            if (includeDesc) pageNicIds.addAll(nicCodeRepository.findDescendantIds(pid));
            else pageNicIds.add(pid);
        }

        // Admin-configured default NIC scope (Company Defaults) — combined
        // with the caller's own selection by intersection (both must match),
        // never blank-filled, same semantics as CompanyController.
        UUID configNicParentId = null;
        if (applyDefaults) {
            for (CompanyDefaultFilter d : companyDefaultFilterService.activeDefaults()) {
                if (!"nic_parent_id".equals(d.getFilterKey())) continue;
                try {
                    JsonNode v = d.getValue() == null || d.getValue().isBlank()
                            ? null : objectMapper.readTree(d.getValue());
                    if (v != null && v.isTextual()) configNicParentId = UUID.fromString(v.asText());
                } catch (Exception e) {
                    // Malformed default value shouldn't kill the request.
                }
            }
        }

        Collection<UUID> nicCodeIds;
        if (configNicParentId != null) {
            LinkedHashSet<UUID> configNicIds = includeDesc
                    ? new LinkedHashSet<>(nicCodeRepository.findDescendantIds(configNicParentId))
                    : new LinkedHashSet<>(List.of(configNicParentId));
            if (!pageParentIds.isEmpty()) {
                configNicIds.retainAll(pageNicIds);
            }
            nicCodeIds = configNicIds;
        } else if (!pageParentIds.isEmpty()) {
            nicCodeIds = pageNicIds;
        } else {
            nicCodeIds = null;
        }

        return companyMapService.companiesInPincode(pincode, radiusKm, nicCodeIds);
    }
}
