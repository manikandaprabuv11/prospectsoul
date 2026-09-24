package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentCandidateEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.repository.EnrichmentCandidateRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.Candidate;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.FactChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactUpdater {

    private final CompanyRepository companyRepository;
    private final EnrichmentCandidateRepository candidateRepository;

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
            case "primary_phone_country" -> company.setPrimaryPhoneCountry(value);
            case "primary_phone_region" -> company.setPrimaryPhoneRegion(value);
            case "primary_phone_carrier" -> company.setPrimaryPhoneCarrier(value);
            case "primary_phone_type" -> company.setPrimaryPhoneType(value);
            case "primary_phone_status" -> company.setPrimaryPhoneStatus(value);
            case "primary_phone_dnd_registered" -> company.setPrimaryPhoneDndRegistered(
                    value != null ? Boolean.parseBoolean(value) : null);
            default -> log.warn("Unknown company field for enrichment: {}", fieldName);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void setCompanyFieldPublic(UUID companyId, String fieldName, String value) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        setCompanyField(company, fieldName, value);
        companyRepository.save(company);
    }

    public record FactUpdateResult(int factsAdded, int factsUpdated, int candidatesAdded) {}
}
