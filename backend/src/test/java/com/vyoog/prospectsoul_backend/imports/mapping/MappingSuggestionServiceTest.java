package com.vyoog.prospectsoul_backend.imports.mapping;

import java.util.List;

import com.vyoog.prospectsoul_backend.imports.normalization.HeaderNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingSuggestionServiceTest {

    private final HeaderNormalizer headerNormalizer = new HeaderNormalizer();
    private final ColumnAliasRegistry registry = new ColumnAliasRegistry(headerNormalizer);
    private final MappingSuggestionService service = new MappingSuggestionService(registry);

    @Test
    void suggestsCorrectMappingsForStandardHeaders() {
        List<String> headers = List.of("Company Name", "Phone Number", "Email", "City", "State");
        var suggestions = service.suggest(headers);

        assertEquals(5, suggestions.size());

        var companyMapping = suggestions.stream()
                .filter(s -> s.sourceColumn().equals("Company Name")).findFirst().orElseThrow();
        assertEquals("canonical_name", companyMapping.targetField());
        assertFalse(companyMapping.ambiguous());

        var phoneMapping = suggestions.stream()
                .filter(s -> s.sourceColumn().equals("Phone Number")).findFirst().orElseThrow();
        assertEquals("primary_phone_normalized", phoneMapping.targetField());
    }

    @Test
    void handlesUnknownHeaders() {
        List<String> headers = List.of("Company Name", "Random Column");
        var suggestions = service.suggest(headers);

        var unknown = suggestions.stream()
                .filter(s -> s.sourceColumn().equals("Random Column")).findFirst().orElseThrow();
        assertNull(unknown.targetField());
        assertEquals(0.0, unknown.confidence());
    }

    @Test
    void doesNotAssignSameTargetTwice() {
        List<String> headers = List.of("Company Name", "Organization Name");
        var suggestions = service.suggest(headers);

        long canonicalCount = suggestions.stream()
                .filter(s -> "canonical_name".equals(s.targetField())).count();
        assertEquals(1, canonicalCount);
    }
}
