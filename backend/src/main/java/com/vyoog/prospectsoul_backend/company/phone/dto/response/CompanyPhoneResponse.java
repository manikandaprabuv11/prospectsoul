package com.vyoog.prospectsoul_backend.company.phone.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceMode;
import com.vyoog.prospectsoul_backend.company.phone.entity.NumberSourceType;
import com.vyoog.prospectsoul_backend.company.phone.entity.PhoneType;

public record CompanyPhoneResponse(
        UUID id,
        String numberRaw,
        String numberNormalized,
        PhoneType phoneType,
        NumberSourceType numberSource,
        ConfidenceLevel confidence,
        ConfidenceMode confidenceMode,
        String designation,
        Boolean isDecisionMaker,
        Boolean isPrimary,
        UUID contactId,
        String contactName,
        String designationOverride,
        String overrideReason,
        String enrichedCountry,
        String enrichedRegion,
        String enrichedCarrier,
        String enrichedLineType,
        String enrichedStatus,
        Boolean enrichedDnd,
        Instant enrichedAt,
        Instant createdAt,
        UUID createdBy
) {}
