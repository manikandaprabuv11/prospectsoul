package com.vyoog.prospectsoul_backend.enrichment.google;

import java.math.BigDecimal;
import java.util.*;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.phone.entity.CompanyPhone;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceMode;
import com.vyoog.prospectsoul_backend.company.phone.entity.NumberSourceType;
import com.vyoog.prospectsoul_backend.company.phone.repository.CompanyPhoneRepository;
import com.vyoog.prospectsoul_backend.company.phone.service.CompanyPhoneService;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.*;
import com.vyoog.prospectsoul_backend.imports.normalization.PhoneNormalizer;
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
    private final CompanyPhoneRepository companyPhoneRepository;
    private final CompanyPhoneService companyPhoneService;
    private final PhoneNormalizer phoneNormalizer;
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
                        Map.of("status", "no_match", "attempts", List.of()));
                return new ProviderResult(ProviderStatus.PARTIAL,
                        List.of(), List.of(), rawPayload, new BigDecimal("0.017"),
                        null, null);
            }

            GooglePlacesFieldMapper.MappingResult mapping = fieldMapper.mapFields(match.place(), company);

            // Intercept phone facts/candidates and create CompanyPhone records directly
            List<FactChange> facts = new ArrayList<>();
            List<Candidate> candidates = new ArrayList<>();

            for (FactChange fact : mapping.facts()) {
                if ("primary_phone_normalized".equals(fact.field())) {
                    storePhoneDirectly(company, fact.newValue());
                } else {
                    facts.add(fact);
                }
            }
            for (Candidate candidate : mapping.candidates()) {
                if ("PHONE_NEW_CONTACT".equals(candidate.candidateType())) {
                    storePhoneDirectly(company, candidate.proposedValue());
                } else {
                    candidates.add(candidate);
                }
            }

            String rawPayload = objectMapper.writeValueAsString(Map.of(
                    "match_method", match.matchMethod(),
                    "attempt_log", match.attemptLog(),
                    "place", objectMapper.readTree(objectMapper.writeValueAsString(match.place()))
            ));

            return new ProviderResult(ProviderStatus.SUCCESS,
                    facts, candidates, rawPayload,
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

    private void storePhoneDirectly(Company company, String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) return;

        String normalized = phoneNormalizer.normalize(rawPhone);
        if (normalized == null || normalized.isBlank()) return;

        boolean exists = companyPhoneRepository
                .findByCompanyIdAndNumberNormalized(company.getId(), normalized)
                .isPresent();
        if (exists) {
            log.debug("Phone {} already exists on company {}, skipping", normalized, company.getId());
            return;
        }

        List<CompanyPhone> existingPhones = companyPhoneRepository
                .findByCompanyIdOrderByIsPrimaryDescConfidenceAscCreatedAtDesc(company.getId());
        boolean isFirst = existingPhones.isEmpty();

        CompanyPhone phone = CompanyPhone.builder()
                .company(company)
                .numberRaw(rawPhone)
                .numberNormalized(normalized)
                .numberSource(NumberSourceType.GOOGLE_API)
                .confidence(ConfidenceLevel.MEDIUM)
                .confidenceMode(ConfidenceMode.AUTO)
                .isPrimary(isFirst)
                .build();
        companyPhoneRepository.save(phone);
        companyPhoneService.recomputePrimary(company.getId());

        log.info("Created CompanyPhone from Google Places: company={} phone={} primary={}",
                company.getId(), normalized, isFirst);
    }
}
