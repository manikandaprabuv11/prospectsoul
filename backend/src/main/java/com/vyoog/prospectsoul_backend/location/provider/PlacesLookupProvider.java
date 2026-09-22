package com.vyoog.prospectsoul_backend.location.provider;

import java.util.List;

import com.vyoog.prospectsoul_backend.location.dto.response.ExternalPlacesResponse;

/**
 * Provider interface for the live external-place search behind the map
 * screen (docs/dev_docs/21 §7). The Google Places implementation lives
 * behind this interface — nothing else in the codebase depends on the
 * Google SDK, and a stub provider is used for local/CI runs.
 */
public interface PlacesLookupProvider {
    List<ExternalPlacesResponse.Result> search(String pincode, int radiusMeters,
                                                String keyword, String type);
    String providerName();
}
