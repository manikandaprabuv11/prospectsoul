package com.vyoog.prospectsoul_backend.enrichment.website;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebsiteCrawler {

    private static final int MAX_PAGES = 5;
    private static final int MAX_TOTAL_BYTES = 1_000_000;
    private static final int MIN_NATIVE_CHARS = 400;
    private static final int MAX_CHARS_PER_PAGE = 35_000;
    private static final Pattern PAGE_RX = Pattern.compile(
            "(about|product|service|industries|industry|capabilit|solution|company|what-we-do)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCLUDE_RX = Pattern.compile(
            "(/blog|/career|/login)", Pattern.CASE_INSENSITIVE);

    private static final List<String> SUBPATH_HINTS = List.of(
            "/about", "/about-us", "/products", "/product", "/services",
            "/industries", "/industry", "/capabilities", "/solutions",
            "/company", "/what-we-do"
    );

    private final NativeFetcher nativeFetcher;
    private final FirecrawlClient firecrawlClient;
    private final ObjectMapper objectMapper;

    public CrawlResult crawl(String websiteUrl) {
        List<PageContent> pages = new ArrayList<>();
        int totalBytes = 0;

        NativeFetcher.FetchResult homePage = nativeFetcher.fetch(websiteUrl);
        boolean reachable = homePage.success();
        if (homePage.success() && homePage.body() != null) {
            String content = truncate(homePage.body());
            pages.add(new PageContent(websiteUrl, content, "native"));
            totalBytes += content.length();
        }

        if (homePage.success()) {
            String baseUrl = normalizeBaseUrl(websiteUrl);
            for (String hint : SUBPATH_HINTS) {
                if (pages.size() >= MAX_PAGES || totalBytes >= MAX_TOTAL_BYTES) break;
                String subUrl = baseUrl + hint;
                if (EXCLUDE_RX.matcher(subUrl).find()) continue;
                NativeFetcher.FetchResult subPage = nativeFetcher.fetch(subUrl);
                if (subPage.success() && subPage.body() != null && subPage.body().length() > 100) {
                    String content = truncate(subPage.body());
                    pages.add(new PageContent(subUrl, content, "native"));
                    totalBytes += content.length();
                }
            }
        }

        boolean poor = !homePage.success() || (homePage.body() != null && homePage.body().length() < MIN_NATIVE_CHARS);
        if (poor && firecrawlClient.isConfigured()) {
            log.info("Native fetch poor for {}, falling back to Firecrawl", websiteUrl);
            try {
                JsonNode scraped = firecrawlClient.scrape(websiteUrl);
                JsonNode data = scraped.get("data");
                if (data != null) {
                    String markdown = data.has("markdown") ? data.get("markdown").asText() : "";
                    if (!markdown.isBlank()) {
                        pages.clear();
                        totalBytes = 0;
                        String content = truncate(markdown);
                        pages.add(new PageContent(websiteUrl, content, "firecrawl"));
                        totalBytes += content.length();
                        reachable = true;
                    }
                }
            } catch (Exception e) {
                log.warn("Firecrawl fallback failed for {}: {}", websiteUrl, e.getMessage());
            }
        }

        return new CrawlResult(reachable, pages, totalBytes);
    }

    private String normalizeBaseUrl(String url) {
        try {
            URI uri = URI.create(url);
            String base = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443) {
                base += ":" + uri.getPort();
            }
            return base;
        } catch (Exception e) {
            return url.replaceAll("/+$", "");
        }
    }

    private String truncate(String text) {
        if (text.length() <= MAX_CHARS_PER_PAGE) return text;
        return text.substring(0, MAX_CHARS_PER_PAGE);
    }

    public record CrawlResult(boolean reachable, List<PageContent> pages, int totalBytes) {}
    public record PageContent(String url, String content, String source) {}
}
