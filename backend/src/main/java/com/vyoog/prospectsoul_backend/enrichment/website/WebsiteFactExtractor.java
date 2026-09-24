package com.vyoog.prospectsoul_backend.enrichment.website;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class WebsiteFactExtractor {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PINCODE_PATTERN = Pattern.compile("\\b[1-9]\\d{5}\\b");
    private static final int MAX_EMAILS = 20;
    private static final int MAX_PHONES = 20;

    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?linkedin\\.com/(?:company|in)/[\\w\\-]+/?");
    private static final Pattern FACEBOOK_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?facebook\\.com/[\\w.\\-]+/?");
    private static final Pattern X_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?(?:twitter\\.com|x\\.com)/[\\w\\-]+/?");
    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?instagram\\.com/[\\w.\\-]+/?");
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?youtube\\.com/(?:@|channel/|c/)[\\w\\-]+/?");

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<title>([^<]{1,500})</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_DESC_PATTERN = Pattern.compile(
            "<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']{1,1000})[\"']",
            Pattern.CASE_INSENSITIVE);

    public ExtractionResult extract(String html) {
        ExtractionResult result = new ExtractionResult();

        Matcher titleMatcher = TITLE_PATTERN.matcher(html);
        if (titleMatcher.find()) {
            result.title = decodeHtmlEntities(titleMatcher.group(1)).trim();
        }

        Matcher descMatcher = META_DESC_PATTERN.matcher(html);
        if (descMatcher.find()) {
            result.description = decodeHtmlEntities(descMatcher.group(1)).trim();
        }

        Set<String> emails = new LinkedHashSet<>();
        Matcher emailMatcher = EMAIL_PATTERN.matcher(html);
        while (emailMatcher.find() && emails.size() < MAX_EMAILS) {
            String email = emailMatcher.group().toLowerCase();
            if (!email.endsWith(".png") && !email.endsWith(".jpg") && !email.endsWith(".gif")) {
                emails.add(email);
            }
        }
        result.emails = new ArrayList<>(emails);

        Set<String> pincodes = new LinkedHashSet<>();
        Matcher pinMatcher = PINCODE_PATTERN.matcher(html);
        while (pinMatcher.find() && pincodes.size() < 10) {
            pincodes.add(pinMatcher.group());
        }
        result.pincodes = new ArrayList<>(pincodes);

        Matcher liMatcher = LINKEDIN_PATTERN.matcher(html);
        if (liMatcher.find()) result.socialLinkedin = liMatcher.group();

        Matcher fbMatcher = FACEBOOK_PATTERN.matcher(html);
        if (fbMatcher.find()) result.socialFacebook = fbMatcher.group();

        Matcher xMatcher = X_PATTERN.matcher(html);
        if (xMatcher.find()) result.socialX = xMatcher.group();

        Matcher igMatcher = INSTAGRAM_PATTERN.matcher(html);
        if (igMatcher.find()) result.socialInstagram = igMatcher.group();

        Matcher ytMatcher = YOUTUBE_PATTERN.matcher(html);
        if (ytMatcher.find()) result.socialYoutube = ytMatcher.group();

        return result;
    }

    private String decodeHtmlEntities(String text) {
        return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
    }

    public static class ExtractionResult {
        public String title;
        public String description;
        public List<String> emails = List.of();
        public List<String> pincodes = List.of();
        public String socialLinkedin;
        public String socialFacebook;
        public String socialX;
        public String socialInstagram;
        public String socialYoutube;
    }
}
