package com.vyoog.prospectsoul_backend.location.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.location.dto.response.MapCompanyResponse;
import com.vyoog.prospectsoul_backend.location.entity.PincodeCentroid;
import com.vyoog.prospectsoul_backend.location.repository.PincodeCentroidRepository;
import com.vyoog.prospectsoul_backend.location.resolution.PincodeResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads owned companies around a pincode centroid, filtered by great-circle
 * radius. Companies with their own {@code (latitude, longitude)} are used
 * as-is; companies without coordinates fall back to the pincode centroid
 * itself (ADR-0009 rationale).
 */
@Service
@RequiredArgsConstructor
public class CompanyMapService {

    private final CompanyRepository companyRepository;
    private final PincodeCentroidRepository pincodeRepository;
    private final PincodeResolutionService pincodeResolver;

    // Fallback centre when no pincode master row and no company coordinates
    // are available — geographic centre of India, so the Google map still
    // opens at a sensible zoom instead of at (0,0) in the Gulf of Guinea.
    private static final BigDecimal INDIA_LAT = new BigDecimal("20.593700");
    private static final BigDecimal INDIA_LNG = new BigDecimal("78.962900");

    @Transactional(readOnly = true)
    public MapCompanyResponse companiesInPincode(String pincode, double radiusKm) {
        // 1. Always show every owned company whose pincode field matches
        //    the request — pincode-equality never depends on the master
        //    centroid table being complete. Companies without coords still
        //    plot; they fall back to whichever centre we resolve below.
        List<Company> pincodeMatches = companyRepository.findAll().stream()
                .filter(c -> pincode.equals(c.getPincode()))
                .toList();

        // 2. Resolve a centre. Prefer the master row (seeded India Post
        //    centroids); else the mean of the pincode's own companies'
        //    coordinates; else India centroid.
        // Cache-first + on-demand external geocode. resolve() throws
        // ResourceNotFoundException for truly unknown pincodes; callers
        // that want a soft fall-back use pincodeResolver.peek(...) instead.
        PincodeCentroid master;
        boolean unknownPincode;
        try {
            master = pincodeResolver.resolve(pincode);
            unknownPincode = false;
        } catch (RuntimeException e) {
            master = null;
            unknownPincode = true;
        }
        BigDecimal centreLat;
        BigDecimal centreLng;
        if (master != null) {
            centreLat = master.getLatitude();
            centreLng = master.getLongitude();
        } else {
            var coordCarrying = pincodeMatches.stream()
                    .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                    .toList();
            if (!coordCarrying.isEmpty()) {
                centreLat = coordCarrying.stream().map(Company::getLatitude)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(coordCarrying.size()), java.math.RoundingMode.HALF_UP);
                centreLng = coordCarrying.stream().map(Company::getLongitude)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(coordCarrying.size()), java.math.RoundingMode.HALF_UP);
            } else {
                centreLat = INDIA_LAT;
                centreLng = INDIA_LNG;
            }
        }

        // 3. Add companies inside the radius that don't share this pincode
        //    but do carry real coordinates.
        final BigDecimal cLat = centreLat;
        final BigDecimal cLng = centreLng;
        List<Company> radiusExtras = companyRepository.findAll().stream()
                .filter(c -> !pincode.equals(c.getPincode()))
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .filter(c -> haversineKm(cLat, cLng, c.getLatitude(), c.getLongitude()) <= radiusKm)
                .toList();

        java.util.LinkedHashMap<java.util.UUID, Company> combined = new java.util.LinkedHashMap<>();
        pincodeMatches.forEach(c -> combined.put(c.getId(), c));
        radiusExtras.forEach(c -> combined.putIfAbsent(c.getId(), c));

        List<MapCompanyResponse.Item> items = combined.values().stream()
                .map(c -> new MapCompanyResponse.Item(
                        c.getId(), c.getCanonicalName(), c.getPipelineState().name(),
                        c.getLatitude()  != null ? c.getLatitude()  : cLat,
                        c.getLongitude() != null ? c.getLongitude() : cLng,
                        c.getPrimaryNicCodeId()))
                .toList();

        return new MapCompanyResponse(
                new MapCompanyResponse.Centre(centreLat, centreLng),
                radiusKm,
                unknownPincode,
                items
        );
    }

    private double haversineKm(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double R = 6371.0088;
        double dLat = Math.toRadians(lat2.subtract(lat1, MathContext.DECIMAL64).doubleValue());
        double dLng = Math.toRadians(lng2.subtract(lng1, MathContext.DECIMAL64).doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
