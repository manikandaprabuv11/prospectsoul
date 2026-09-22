package com.vyoog.prospectsoul_backend.imports.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Parses the Udyam registry {@code Activities} column, which is a JSON
 * array of {@code {NIC5DigitId, Description}} objects. Per Kickoff §Tests
 * the parser MUST handle: valid array, single-element, literal {@code "NA"},
 * empty string / null, and malformed JSON.
 *
 * The malformed case must NOT throw — the caller flags the import row with
 * {@code outcome_reason = activities_json_invalid} and continues.
 */
@Component
@RequiredArgsConstructor
public class ActivitiesJsonParser {

    public record Activity(String nicCode, String description) {}

    public sealed interface Result {
        record Ok(List<Activity> activities) implements Result {}
        record Empty() implements Result {}
        record Invalid(String reason) implements Result {}
    }

    private final ObjectMapper objectMapper;

    public Result parse(String raw) {
        if (raw == null) return new Result.Empty();
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "NA".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return new Result.Empty();
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (!node.isArray()) {
                return new Result.Invalid("root is not an array");
            }
            if (node.isEmpty()) return new Result.Empty();
            List<Activity> out = new ArrayList<>();
            for (JsonNode elem : node) {
                if (!elem.isObject()) continue;
                String code = firstNonBlank(
                        elem.path("NIC5DigitId").asText(null),
                        elem.path("nic_5_digit_id").asText(null),
                        elem.path("Nic5DigitId").asText(null),
                        elem.path("NicCode").asText(null),
                        elem.path("code").asText(null)
                );
                String desc = firstNonBlank(
                        elem.path("Description").asText(null),
                        elem.path("description").asText(null),
                        elem.path("Activity").asText(null)
                );
                if (code == null || code.isBlank()) continue;
                out.add(new Activity(code.trim(), desc == null ? null : desc.trim()));
            }
            if (out.isEmpty()) return new Result.Empty();
            return new Result.Ok(Collections.unmodifiableList(out));
        } catch (JacksonException e) {
            return new Result.Invalid(e.getOriginalMessage());
        } catch (RuntimeException e) {
            return new Result.Invalid(e.getMessage());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank() && !"null".equalsIgnoreCase(v)) return v;
        }
        return null;
    }
}
