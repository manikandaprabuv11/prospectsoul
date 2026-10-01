package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.phone.entity.CompanyPhone;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceMode;
import com.vyoog.prospectsoul_backend.company.phone.entity.NumberSourceType;
import com.vyoog.prospectsoul_backend.company.phone.repository.CompanyPhoneRepository;
import com.vyoog.prospectsoul_backend.company.phone.service.ConfidenceEngine;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentCandidateEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.repository.EnrichmentCandidateRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.Candidate;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.FactChange;
import com.vyoog.prospectsoul_backend.imports.normalization.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactUpdater {

    private static final Map<String, NumberSourceType> PROVIDER_SOURCE_MAP = Map.of(
            "GOOGLE_PLACES", NumberSourceType.GOOGLE_API,
            "WEBSITE", NumberSourceType.WEBSITE,
            "LINKEDIN", NumberSourceType.LINKEDIN,
            "INDIAMART", NumberSourceType.INDIAMART
    );

    private final CompanyRepository companyRepository;
    private final EnrichmentCandidateRepository candidateRepository;
    private final CompanyPhoneRepository companyPhoneRepository;
    private final ConfidenceEngine confidenceEngine;
    private final PhoneNormalizer phoneNormalizer;

    @Transactional(propagation = Propagation.MANDATORY)
    public FactUpdateResult applyFacts(UUID companyId, UUID jobId, String providerKey,
                                        List<FactChange> facts, List<Candidate> candidates) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        int added = 0;
        int updated = 0;
        int candidatesAdded = 0;

        for (FactChange fact : facts) {
            if (fact.entity() == FactChange.FactEntity.COMPANY) {
                if (fact.overwritesHuman()) {
                    candidateRepository.save(EnrichmentCandidateEntity.builder()
                            .companyId(companyId)
                            .enrichmentJobId(jobId)
                            .candidateType("FIELD_OVERWRITE")
                            .fieldName(fact.field())
                            .proposedValue(fact.newValue())
                            .currentValue(fact.oldValue())
                            .providerKey(providerKey)
                            .build());
                    candidatesAdded++;
                } else {
                    boolean wasEmpty = fact.oldValue() == null || fact.oldValue().isBlank();
                    setCompanyField(company, fact.field(), fact.newValue());
                    if ("primary_phone_normalized".equals(fact.field()) && fact.newValue() != null) {
                        ensureCompanyPhoneRecord(company, fact.newValue(), providerKey);
                    }
                    if (wasEmpty) added++;
                    else updated++;
                }
            }
        }

        for (Candidate candidate : candidates) {
            candidateRepository.save(EnrichmentCandidateEntity.builder()
                    .companyId(companyId)
                    .enrichmentJobId(jobId)
                    .candidateType(candidate.candidateType())
                    .fieldName(candidate.fieldName())
                    .proposedValue(candidate.proposedValue())
                    .currentValue(candidate.currentValue())
                    .providerKey(providerKey)
                    .build());
            candidatesAdded++;
        }

        companyRepository.save(company);
        return new FactUpdateResult(added, updated, candidatesAdded);
    }

    private void ensureCompanyPhoneRecord(Company company, String phoneValue, String providerKey) {
        String normalized = phoneNormalizer.normalize(phoneValue);
        if (normalized == null || normalized.isBlank()) return;

        boolean exists = companyPhoneRepository
                .findByCompanyIdAndNumberNormalized(company.getId(), normalized)
                .isPresent();
        if (exists) return;

        NumberSourceType source = PROVIDER_SOURCE_MAP.getOrDefault(providerKey, NumberSourceType.GOOGLE_API);
        ConfidenceEngine.ComputeResult conf = confidenceEngine.compute(
                source, null, null, null, null);

        boolean hasOtherPhones = !companyPhoneRepository
                .findByCompanyIdOrderByIsPrimaryDescConfidenceAscCreatedAtDesc(company.getId())
                .isEmpty();

        CompanyPhone phone = CompanyPhone.builder()
                .company(company)
                .numberRaw(phoneValue)
                .numberNormalized(normalized)
                .numberSource(source)
                .confidence(ConfidenceLevel.max(conf.confidence(), ConfidenceLevel.MEDIUM))
                .confidenceMode(ConfidenceMode.AUTO)
                .isPrimary(!hasOtherPhones)
                .build();
        companyPhoneRepository.save(phone);

        log.info("Created CompanyPhone from enrichment provider={} for company={}, confidence={}",
                providerKey, company.getId(), phone.getConfidence());
    }

    private void setCompanyField(Company company, String fieldName, String value) {
        switch (fieldName) {
            case "google_place_id" -> company.setGooglePlaceId(value);
            case "google_name" -> company.setGoogleName(value);
            case "google_business_category" -> company.setGoogleBusinessCategory(value);
            case "google_business_types" -> company.setGoogleBusinessTypes(value);
            case "google_maps_url" -> company.setGoogleMapsUrl(value);
            case "google_lat" -> { if (value != null) company.setGoogleLat(new java.math.BigDecimal(value)); }
            case "google_lng" -> { if (value != null) company.setGoogleLng(new java.math.BigDecimal(value)); }
            case "google_business_status" -> company.setGoogleBusinessStatus(value);
            case "address_line" -> company.setAddressLine(value);
            case "primary_phone_normalized" -> company.setPrimaryPhoneNormalized(value);
            case "website_domain" -> company.setWebsiteDomain(value);
            case "website_reachable" -> company.setWebsiteReachable(value != null ? Boolean.parseBoolean(value) : null);
            case "website_title" -> company.setWebsiteTitle(value);
            case "website_description" -> company.setWebsiteDescription(value);
            case "social_linkedin" -> company.setSocialLinkedin(value);
            case "social_facebook" -> company.setSocialFacebook(value);
            case "social_x" -> company.setSocialX(value);
            case "social_instagram" -> company.setSocialInstagram(value);
            case "social_youtube" -> company.setSocialYoutube(value);
            case "products" -> company.setProducts(value);
            case "primary_phone_country", "primary_phone_region", "primary_phone_carrier",
                 "primary_phone_type", "primary_phone_status", "primary_phone_dnd_registered" ->
                    log.debug("Phone enrichment field '{}' now lives on company_phones; skipping company update", fieldName);
            default -> log.warn("Unknown company field for enrichment: {}", fieldName);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void setCompanyFieldPublic(UUID companyId, String fieldName, String value) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        setCompanyField(company, fieldName, value);
        if ("primary_phone_normalized".equals(fieldName) && value != null) {
            ensureCompanyPhoneRecord(company, value, "MANUAL");
        }
        companyRepository.save(company);
    }

    public record FactUpdateResult(int factsAdded, int factsUpdated, int candidatesAdded) {}
}
