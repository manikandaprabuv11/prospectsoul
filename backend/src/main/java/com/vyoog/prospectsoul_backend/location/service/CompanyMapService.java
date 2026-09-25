package com.vyoog.prospectsoul_backend.location.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.location.dto.response.MapCompanyResponse;
import com.vyoog.prospectsoul_backend.location.entity.PincodeCentroid;
import com.vyoog.prospectsoul_backend.location.repository.PincodeCentroidRepository;
import com.vyoog.prospectsoul_backend.location.resolution.PincodeResolutionService;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads owned companies around a pincode centroid, filtered by great-circle
 * radius and, optionally, by NIC code — filters AND together (Companies Map
 * NIC filter ticket): a pincode/radius match must ALSO carry a matching NIC
 * code when {@code nicParentId} is supplied. NIC resolution and matching
 * reuse the same descendant-expansion (recursive CTE) and join-table
 * membership check as the Companies list filter
 * ({@link com.vyoog.prospectsoul_backend.company.specification.CompanySpecification}),
 * so the two screens' NIC filter behave identically.
 */
@Service
@RequiredArgsConstructor
public class CompanyMapService {

    private final CompanyRepository companyRepository;
    private final PincodeCentroidRepository pincodeRepository;
    private final PincodeResolutionService pincodeResolver;
    private final CompanyNicCodeRepository companyNicCodeRepository;
    private final NicCodeRepository nicCodeRepository;

    // Fallback centre when no pincode master row and no company coordinates
    // are available — geographic centre of India, so the Google map still
    // opens at a sensible zoom instead of at (0,0) in the Gulf of Guinea.
    private static final BigDecimal INDIA_LAT = new BigDecimal("20.593700");
    private static final BigDecimal INDIA_LNG = new BigDecimal("78.962900");

    @Transactional(readOnly = true)
    public MapCompanyResponse companiesInPincode(String pincode, double radiusKm) {
        return companiesInPincode(pincode, radiusKm, null, null);
    }

    /**
     * Delegating shim kept for backwards compatibility (existing callers,
     * including {@link com.vyoog.prospectsoul_backend.location.LocationIntegrationTest}):
     * resolves the single parent id to its descendant subtree — exactly the
     * pre-multi-NIC behaviour — then forwards to the pre-resolved overload.
     */
    @Transactional(readOnly = true)
    public MapCompanyResponse companiesInPincode(String pincode, double radiusKm,
                                                  UUID nicParentId, Boolean nicIncludeDescendants) {
        Collection<UUID> nicCodeIds = null;
        if (nicParentId != null) {
            boolean includeDesc = nicIncludeDescendants == null || nicIncludeDescendants;
            nicCodeIds = includeDesc
                    ? nicCodeRepository.findDescendantIds(nicParentId)
                    : List.of(nicParentId);
        }
        return companiesInPincode(pincode, radiusKm, nicCodeIds);
    }

    /**
     * Main entry point once the caller (MapController) has already resolved
     * the NIC selection to a concrete set of NIC code ids — multi-parent
     * OR-expansion and the config-default intersection both happen there, the
     * same shape as {@link com.vyoog.prospectsoul_backend.company.specification.CompanySpecification}'s
     * NIC filter — so this service only needs to apply the join-table
     * membership check. {@code null} means "no NIC filter active"; an empty
     * (non-null) collection means the filter resolved to nothing and every
     * company should be excluded — that is a correct empty result, not a bug.
     */
    @Transactional(readOnly = true)
    public MapCompanyResponse companiesInPincode(String pincode, double radiusKm,
                                                  Collection<UUID> nicCodeIds) {
        Set<UUID> matchedNicCodeIds = nicCodeIds == null ? null : Set.copyOf(nicCodeIds);
        Set<UUID> nicCompanyIds = nicCodeIds == null
                ? null
                : (nicCodeIds.isEmpty()
                        ? Set.of()
                        : Set.copyOf(companyNicCodeRepository.findCompanyIdsByNicCodeIdIn(nicCodeIds)));
        final Set<UUID> nicFilter = nicCompanyIds;
        final List<UUID> matchedNicCodeIdList = matchedNicCodeIds == null ? null : List.copyOf(matchedNicCodeIds);

        // 1. Always show every owned company whose pincode field matches
        //    the request — pincode-equality never depends on the master
        //    centroid table being complete. Companies without coords still
        //    plot; they fall back to whichever centre we resolve below.
        List<Company> pincodeMatches = companyRepository.findAll().stream()
                .filter(c -> pincode.equals(c.getPincode()))
                .filter(c -> nicFilter == null || nicFilter.contains(c.getId()))
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
                .filter(c -> nicFilter == null || nicFilter.contains(c.getId()))
                .toList();

        java.util.LinkedHashMap<java.util.UUID, Company> combined = new java.util.LinkedHashMap<>();
        pincodeMatches.forEach(c -> combined.put(c.getId(), c));
        radiusExtras.forEach(c -> combined.putIfAbsent(c.getId(), c));

        // Bulk-fetch every matched company's NIC codes (primary + secondary)
        // in one query — same pattern as CompanyService's list enrichment —
        // so the map's company cards can show NIC chips without an N+1.
        // Always the FULL, unfiltered set: which chips are visible for the
        // active filter is a display decision the frontend makes using
        // matchedNicCodeIds below, not something baked into this list.
        Map<UUID, List<MapCompanyResponse.NicCodeRef>> nicByCompany = new java.util.HashMap<>();
        if (!combined.isEmpty()) {
            for (CompanyNicCode cnc : companyNicCodeRepository
                    .findByCompanyIdInOrderByCompanyIdAscSequenceNoAsc(List.copyOf(combined.keySet()))) {
                NicCode nc = cnc.getNicCode();
                UUID nicId = nc != null ? nc.getId() : null;
                String code = nc != null ? nc.getCode() : cnc.getNicCodeRaw();
                String desc = nc != null ? nc.getDescription() : cnc.getDescriptionRaw();
                nicByCompany.computeIfAbsent(cnc.getCompanyId(), k -> new java.util.ArrayList<>())
                        .add(new MapCompanyResponse.NicCodeRef(nicId, code, desc, Boolean.TRUE.equals(cnc.getIsPrimary())));
            }
        }

        List<MapCompanyResponse.Item> items = combined.values().stream()
                .map(c -> new MapCompanyResponse.Item(
                        c.getId(), c.getCanonicalName(), c.getPipelineState().name(),
                        c.getLatitude()  != null ? c.getLatitude()  : cLat,
                        c.getLongitude() != null ? c.getLongitude() : cLng,
                        c.getPrimaryNicCodeId(),
                        nicByCompany.getOrDefault(c.getId(), List.of())))
                .toList();

        return new MapCompanyResponse(
                new MapCompanyResponse.Centre(centreLat, centreLng),
                radiusKm,
                unknownPincode,
                items,
                matchedNicCodeIdList
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
