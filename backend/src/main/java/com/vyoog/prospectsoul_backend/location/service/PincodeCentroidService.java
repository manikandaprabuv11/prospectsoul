package com.vyoog.prospectsoul_backend.location.service;

import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.location.dto.response.PincodeCentroidResponse;
import com.vyoog.prospectsoul_backend.location.entity.PincodeCentroid;
import com.vyoog.prospectsoul_backend.location.resolution.PincodeResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thin controller-facing service. Delegates to
 * {@link PincodeResolutionService} — cache first, external geocoder on
 * miss, persistent upsert. Never throws on a legitimate 6-digit pincode
 * that India Post knows.
 */
@Service
@RequiredArgsConstructor
public class PincodeCentroidService {

    private final PincodeResolutionService pincodeResolver;

    @Transactional
    public PincodeCentroidResponse lookup(String pincode) {
        if (pincode == null || !pincode.matches("\\d{6}")) {
            throw new BusinessRuleException("pincode must be a 6-digit string");
        }
        PincodeCentroid p = pincodeResolver.resolve(pincode);
        return new PincodeCentroidResponse(
                p.getPincode(), p.getAreaName(),
                p.getDistrict(), p.getState(),
                new PincodeCentroidResponse.Centroid(p.getLatitude(), p.getLongitude()));
    }
}
