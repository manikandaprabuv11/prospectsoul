package com.vyoog.prospectsoul_backend.enrichment.website;

import java.math.BigDecimal;
import java.util.*;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.*;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.FactChange.FactEntity;
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

            facts.add(new FactChange(FactEntity.COMPANY, "website_reachable",
                    company.getWebsiteReachable() != null ? company.getWebsiteReachable().toString() : null,
                    String.valueOf(crawlResult.reachable()), false));

            if (!crawlResult.pages().isEmpty()) {
                String allHtml = crawlResult.pages().stream()
                        .map(WebsiteCrawler.PageContent::content)
                        .reduce("", (a, b) -> a + "\n" + b);

                WebsiteFactExtractor.ExtractionResult extraction = extractor.extract(allHtml);

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
            }

            String rawPayload;
            try {
                rawPayload = objectMapper.writeValueAsString(Map.of(
                        "reachable", crawlResult.reachable(),
                        "pages_crawled", crawlResult.pages().size(),
                        "total_bytes", crawlResult.totalBytes()
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
