package com.vyoog.prospectsoul_backend.imports.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MappingSuggestionService {

    private final ColumnAliasRegistry aliasRegistry;

    public record MappingSuggestion(
            String sourceColumn,
            String targetField,
            double confidence,
            String matchType,
            boolean ambiguous
    ) {}

    public List<MappingSuggestion> suggest(List<String> sourceHeaders) {
        List<MappingSuggestion> suggestions = new ArrayList<>();
        Set<String> assignedTargets = new HashSet<>();

        // first pass: high-confidence matches
        for (String header : sourceHeaders) {
            ColumnAliasRegistry.MatchResult result = aliasRegistry.match(header);
            if (result != null && result.confidence() >= 0.8 && !assignedTargets.contains(result.targetField())) {
                suggestions.add(new MappingSuggestion(
                        header, result.targetField(), result.confidence(), result.matchType(), false));
                assignedTargets.add(result.targetField());
            }
        }

        // second pass: lower-confidence matches for unmapped headers
        for (String header : sourceHeaders) {
            boolean alreadyMapped = suggestions.stream().anyMatch(s -> s.sourceColumn().equals(header));
            if (alreadyMapped) continue;

            ColumnAliasRegistry.MatchResult result = aliasRegistry.match(header);
            if (result != null && !assignedTargets.contains(result.targetField())) {
                boolean ambiguous = result.confidence() < 0.8;
                suggestions.add(new MappingSuggestion(
                        header, result.targetField(), result.confidence(), result.matchType(), ambiguous));
                assignedTargets.add(result.targetField());
            } else {
                suggestions.add(new MappingSuggestion(header, null, 0.0, "NONE", false));
            }
        }

        return suggestions;
    }
}
