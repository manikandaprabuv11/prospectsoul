package com.vyoog.prospectsoul_backend.location.controller;

import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.location.dto.response.MapCompanyResponse;
import com.vyoog.prospectsoul_backend.location.dto.response.PincodeCentroidResponse;
import com.vyoog.prospectsoul_backend.location.service.CompanyMapService;
import com.vyoog.prospectsoul_backend.location.service.PincodeCentroidService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class MapController {

    private final PincodeCentroidService pincodeCentroidService;
    private final CompanyMapService companyMapService;

    @GetMapping("/pincode/{pincode}")
    @PreAuthorize(RoleConstants.HAS_READ)
    public PincodeCentroidResponse pincode(@PathVariable String pincode) {
        return pincodeCentroidService.lookup(pincode);
    }

    @GetMapping("/companies")
    @PreAuthorize(RoleConstants.HAS_READ)
    public MapCompanyResponse companies(@RequestParam String pincode,
                                         @RequestParam(name = "radius_km", defaultValue = "5") double radiusKm,
                                         @RequestParam(name = "nic_parent_id", required = false) UUID nicParentId,
                                         @RequestParam(name = "nic_include_descendants", required = false) Boolean nicIncludeDescendants) {
        return companyMapService.companiesInPincode(pincode, radiusKm, nicParentId, nicIncludeDescendants);
    }
}
