package com.vyoog.prospectsoul_backend.location.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.location.dto.response.ExternalPlacesResponse;
import com.vyoog.prospectsoul_backend.location.provider.PlacesLookupProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * External-place lookup wrapper (docs 21 §7). Enforces:
 *   - a per-day quota (Admin-configurable, YAML for now);
 *   - never writing anything to any table (Domain Model Addendum
 *     Invariant 15);
 *   - a stable JSON shape with {@code persisted: false} for the consumer.
 */
@Service
@RequiredArgsConstructor
public class PlacesLookupService {

    private final PlacesLookupProvider provider;

    @Value("${prospectsoul.places.daily-quota:5000}")
    private int dailyQuota;

    @Value("${prospectsoul.places.default-radius-m:3000}")
    private int defaultRadiusMeters;

    private final AtomicInteger dailyUsed = new AtomicInteger();

    public ExternalPlacesResponse search(String pincode, Integer radiusMeters, String keyword, String type) {
        if (pincode == null || !pincode.matches("\\d{6}")) {
            throw new BusinessRuleException("pincode must be 6 digits");
        }
        int used = dailyUsed.incrementAndGet();
        if (used > dailyQuota) {
            throw new BusinessRuleException(
                    "Daily Places quota exceeded (" + dailyQuota + ") — try again after midnight UTC");
        }
        int r = radiusMeters == null ? defaultRadiusMeters : radiusMeters;
        List<ExternalPlacesResponse.Result> results = provider.search(pincode, r, keyword, type);
        int remaining = Math.max(0, dailyQuota - used);
        return new ExternalPlacesResponse(results, provider.providerName(), false, remaining);
    }

    /** Test helper — reset counter between test methods. */
    public void resetQuotaCounter() { dailyUsed.set(0); }
}
