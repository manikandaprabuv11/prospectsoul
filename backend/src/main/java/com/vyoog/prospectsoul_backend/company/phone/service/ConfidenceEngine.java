package com.vyoog.prospectsoul_backend.company.phone.service;

import java.util.Map;

import com.vyoog.prospectsoul_backend.admin.designation.service.DecisionMakerDesignationService;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceMode;
import com.vyoog.prospectsoul_backend.company.phone.entity.NumberSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfidenceEngine {

    private static final Map<NumberSourceType, ConfidenceLevel> SOURCE_CONFIDENCE_MAP = Map.of(
            NumberSourceType.BUSINESS_CARD, ConfidenceLevel.HIGH,
            NumberSourceType.FIELD_VISIT, ConfidenceLevel.HIGH,
            NumberSourceType.REFERENCE, ConfidenceLevel.HIGH,
            NumberSourceType.WEBSITE, ConfidenceLevel.MEDIUM,
            NumberSourceType.GOOGLE_API, ConfidenceLevel.MEDIUM,
            NumberSourceType.LINKEDIN, ConfidenceLevel.MEDIUM,
            NumberSourceType.INDIAMART, ConfidenceLevel.LOW,
            NumberSourceType.IMPORT_DEFAULT, ConfidenceLevel.MEDIUM
    );

    private final DecisionMakerDesignationService designationService;

    public record ComputeResult(ConfidenceLevel confidence, ConfidenceMode mode, boolean isDecisionMaker) {}

    /**
     * Compute confidence for a phone number based on source, designation,
     * and existing confidence. Implements all rules from Doc 35.
     *
     * @param source            the number source
     * @param designation       resolved designation (from contact or override)
     * @param existingConfidence current confidence (null for new phone)
     * @param existingMode      current confidence mode (null for new phone)
     * @param userSelectedConfidence confidence selected by user (only for MANUAL_ENTRY)
     */
    public ComputeResult compute(NumberSourceType source,
                                  String designation,
                                  ConfidenceLevel existingConfidence,
                                  ConfidenceMode existingMode,
                                  ConfidenceLevel userSelectedConfidence) {

        // Rule 0: Manual overrides are never auto-changed
        if (existingMode == ConfidenceMode.MANUAL) {
            return new ComputeResult(existingConfidence, ConfidenceMode.MANUAL,
                    designationService.isDecisionMaker(designation));
        }

        boolean isDm = designationService.isDecisionMaker(designation);

        // Rule 1: For MANUAL_ENTRY, use user-selected confidence
        if (source == NumberSourceType.MANUAL_ENTRY) {
            ConfidenceLevel selected = userSelectedConfidence != null ? userSelectedConfidence : ConfidenceLevel.MEDIUM;
            ConfidenceMode mode = (userSelectedConfidence != null && userSelectedConfidence != ConfidenceLevel.MEDIUM)
                    ? ConfidenceMode.MANUAL : ConfidenceMode.AUTO;

            // Apply designation override even for manual entry
            if (isDm) {
                ConfidenceLevel dmLevel = ConfidenceLevel.HIGH;
                selected = ConfidenceLevel.max(selected, dmLevel);
            }

            // Never-downgrade
            if (existingConfidence != null) {
                selected = ConfidenceLevel.max(existingConfidence, selected);
            }

            return new ComputeResult(selected, mode, isDm);
        }

        // Rule 2: Compute base confidence from source
        ConfidenceLevel baseConfidence = SOURCE_CONFIDENCE_MAP.getOrDefault(source, ConfidenceLevel.MEDIUM);

        // Rule 3: Apply designation override
        if (isDm) {
            ConfidenceLevel designationConfidence;
            if (source == NumberSourceType.INDIAMART) {
                designationConfidence = ConfidenceLevel.MEDIUM;
            } else {
                designationConfidence = ConfidenceLevel.HIGH;
            }
            baseConfidence = ConfidenceLevel.max(baseConfidence, designationConfidence);
        }

        // Rule 4: Never downgrade
        if (existingConfidence != null) {
            baseConfidence = ConfidenceLevel.max(existingConfidence, baseConfidence);
        }

        return new ComputeResult(baseConfidence, ConfidenceMode.AUTO, isDm);
    }

    public ConfidenceLevel getBaseConfidence(NumberSourceType source) {
        return SOURCE_CONFIDENCE_MAP.getOrDefault(source, ConfidenceLevel.MEDIUM);
    }
}
