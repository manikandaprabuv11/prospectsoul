package com.vyoog.prospectsoul_backend.location.controller;

import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.location.dto.response.ExternalPlacesResponse;
import com.vyoog.prospectsoul_backend.location.service.PlacesLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/external")
@RequiredArgsConstructor
public class PlacesLookupController {

    private final PlacesLookupService placesLookupService;

    @GetMapping("/places-search")
    @PreAuthorize(RoleConstants.HAS_READ)
    public ExternalPlacesResponse search(@RequestParam String pincode,
                                          @RequestParam(name = "radius_m", required = false) Integer radiusMeters,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String type) {
        return placesLookupService.search(pincode, radiusMeters, keyword, type);
    }
}
