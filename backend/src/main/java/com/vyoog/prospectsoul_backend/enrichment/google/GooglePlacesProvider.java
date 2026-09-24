package com.vyoog.prospectsoul_backend.enrichment.google;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesProvider implements EnrichmentProvider {

    public static final String KEY = "GOOGLE_PLACES";

    private final GooglePlacesClient client;
    private final GooglePlacesMatcher matcher;
    private final GooglePlacesFieldMapper fieldMapper;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.COMPANY_ENRICHMENT);
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        UUID companyId = request.companyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        if (!client.isConfigured()) {
            return new ProviderResult(ProviderStatus.FAILED_PERMANENT,
                    List.of(), List.of(), null, BigDecimal.ZERO,
                    "NOT_CONFIGURED", "Google Places API key not configured");
        }

        try {
            GooglePlacesMatcher.MatchResult match = matcher.findBestMatch(
                    company.getGooglePlaceId(),
                    company.getCanonicalName(),
                    company.getPincode(),
                    company.getCity(),
                    company.getState()
            );

            if (match == null) {
                String rawPayload = objectMapper.writeValueAsString(
                        java.util.Map.of("status", "no_match", "attempts", List.of()));
                return new ProviderResult(ProviderStatus.PARTIAL,
                        List.of(), List.of(), rawPayload, new BigDecimal("0.017"),
                        null, null);
            }

            GooglePlacesFieldMapper.MappingResult mapping = fieldMapper.mapFields(match.place(), company);

            String rawPayload = objectMapper.writeValueAsString(java.util.Map.of(
                    "match_method", match.matchMethod(),
                    "attempt_log", match.attemptLog(),
                    "place", objectMapper.readTree(objectMapper.writeValueAsString(match.place()))
            ));

            return new ProviderResult(ProviderStatus.SUCCESS,
                    mapping.facts(), mapping.candidates(), rawPayload,
                    new BigDecimal("0.017"), null, null);

        } catch (GooglePlacesClient.GooglePlacesRateLimitException e) {
            return new ProviderResult(ProviderStatus.FAILED_TRANSIENT,
                    List.of(), List.of(), null, BigDecimal.ZERO,
                    "RATE_LIMITED", e.getMessage());

        } catch (GooglePlacesClient.GooglePlacesTransientException e) {
            return new ProviderResult(ProviderStatus.FAILED_TRANSIENT,
                    List.of(), List.of(), null, BigDecimal.ZERO,
                    "TRANSIENT_ERROR", e.getMessage());

        } catch (GooglePlacesClient.GooglePlacesPermanentException e) {
            return new ProviderResult(ProviderStatus.FAILED_PERMANENT,
                    List.of(), List.of(), null, BigDecimal.ZERO,
                    "PERMANENT_ERROR", e.getMessage());

        } catch (Exception e) {
            log.error("Google Places enrichment failed for company {}", companyId, e);
            return new ProviderResult(ProviderStatus.FAILED_TRANSIENT,
                    List.of(), List.of(), null, BigDecimal.ZERO,
                    "EXCEPTION", e.getMessage());
        }
    }
}
