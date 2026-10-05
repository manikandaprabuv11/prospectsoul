package com.vyoog.prospectsoul_backend.enrichment.website;

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
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.FactChange.FactEntity;
import com.vyoog.prospectsoul_backend.imports.normalization.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebsiteProvider implements EnrichmentProvider {

    public static final String KEY = "WEBSITE";

    private final WebsiteCrawler crawler;
    private final WebsiteFactExtractor extractor;
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
        return Set.of(ProviderCapability.WEB_SCRAPE, ProviderCapability.COMPANY_ENRICHMENT);
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        UUID companyId = request.companyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        String websiteUrl = company.getWebsiteDomain();
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return new ProviderResult(ProviderStatus.FAILED_PERMANENT,
                    List.of(), List.of(), null, BigDecimal.ZERO,
                    "NO_WEBSITE", "Company has no website URL");
        }

        if (!websiteUrl.startsWith("http")) {
            websiteUrl = "https://" + websiteUrl;
        }

        try {
            WebsiteCrawler.CrawlResult crawlResult = crawler.crawl(websiteUrl);

            List<FactChange> facts = new ArrayList<>();
            List<Candidate> candidates = new ArrayList<>();
            int phonesAdded = 0;

            facts.add(new FactChange(FactEntity.COMPANY, "website_reachable",
                    company.getWebsiteReachable() != null ? company.getWebsiteReachable().toString() : null,
                    String.valueOf(crawlResult.reachable()), false));

            if (!crawlResult.pages().isEmpty()) {
                String allContent = crawlResult.pages().stream()
                        .map(WebsiteCrawler.PageContent::content)
                        .reduce("", (a, b) -> a + "\n" + b);

                WebsiteFactExtractor.ExtractionResult extraction = extractor.extract(allContent);

                if (extraction.title != null) {
                    facts.add(new FactChange(FactEntity.COMPANY, "website_title",
                            company.getWebsiteTitle(), extraction.title, false));
                }
                if (extraction.description != null) {
                    facts.add(new FactChange(FactEntity.COMPANY, "website_description",
                            company.getWebsiteDescription(), extraction.description, false));
                }

                if (extraction.socialLinkedin != null) {
                    facts.add(new FactChange(FactEntity.COMPANY, "social_linkedin",
                            company.getSocialLinkedin(), extraction.socialLinkedin, false));
                }
                if (extraction.socialFacebook != null) {
                    facts.add(new FactChange(FactEntity.COMPANY, "social_facebook",
                            company.getSocialFacebook(), extraction.socialFacebook, false));
                }
                if (extraction.socialX != null) {
                    facts.add(new FactChange(FactEntity.COMPANY, "social_x",
                            company.getSocialX(), extraction.socialX, false));
                }
                if (extraction.socialInstagram != null) {
                    facts.add(new FactChange(FactEntity.COMPANY, "social_instagram",
                            company.getSocialInstagram(), extraction.socialInstagram, false));
                }
                if (extraction.socialYoutube != null) {
                    facts.add(new FactChange(FactEntity.COMPANY, "social_youtube",
                            company.getSocialYoutube(), extraction.socialYoutube, false));
                }

                for (String email : extraction.emails) {
                    candidates.add(new Candidate("EMAIL_NEW_CONTACT", "email", email, null));
                }

                // Store every extracted phone directly as a CompanyPhone record
                List<CompanyPhone> existingPhones = companyPhoneRepository
                        .findByCompanyIdOrderByIsPrimaryDescConfidenceAscCreatedAtDesc(companyId);
                Set<String> existingNormalized = new HashSet<>();
                for (CompanyPhone ep : existingPhones) {
                    if (ep.getNumberNormalized() != null) {
                        existingNormalized.add(ep.getNumberNormalized());
                    }
                }

                for (String rawPhone : extraction.phones) {
                    String normalized = phoneNormalizer.normalize(rawPhone);
                    if (normalized == null || normalized.isBlank()) continue;
                    if (existingNormalized.contains(normalized)) {
                        log.debug("Phone {} already exists on company {}, skipping", normalized, companyId);
                        continue;
                    }

                    boolean isFirst = existingPhones.isEmpty() && phonesAdded == 0;
                    CompanyPhone phone = CompanyPhone.builder()
                            .company(company)
                            .numberRaw(rawPhone)
                            .numberNormalized(normalized)
                            .numberSource(NumberSourceType.WEBSITE)
                            .confidence(ConfidenceLevel.MEDIUM)
                            .confidenceMode(ConfidenceMode.AUTO)
                            .isPrimary(isFirst)
                            .build();
                    companyPhoneRepository.save(phone);
                    existingNormalized.add(normalized);
                    phonesAdded++;

                    log.info("Created CompanyPhone from website scrape: company={} phone={} primary={}",
                            companyId, normalized, isFirst);
                }

                if (phonesAdded > 0) {
                    companyPhoneService.recomputePrimary(companyId);
                }
            }

            String rawPayload;
            try {
                rawPayload = objectMapper.writeValueAsString(Map.of(
                        "reachable", crawlResult.reachable(),
                        "pages_crawled", crawlResult.pages().size(),
                        "total_bytes", crawlResult.totalBytes(),
                        "phones_added", phonesAdded
                ));
            } catch (Exception e) {
                rawPayload = "{\"error\": \"serialization_failed\"}";
            }

            return new ProviderResult(
                    crawlResult.reachable() ? ProviderStatus.SUCCESS : ProviderStatus.PARTIAL,
                    facts, candidates, rawPayload,
                    new BigDecimal("0.002"), null, null);

        } catch (Exception e) {
            log.error("Website enrichment failed for company {}", companyId, e);
            return new ProviderResult(ProviderStatus.FAILED_TRANSIENT,
                    List.of(), List.of(), null, BigDecimal.ZERO,
                    "EXCEPTION", e.getMessage());
        }
    }
}
