package com.vyoog.prospectsoul_backend.enrichment.phone;

import java.util.ArrayList;
import java.util.List;

import com.vyoog.prospectsoul_backend.enrichment.framework.spi.Candidate;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.FactChange;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.FactChange.FactEntity;
import org.springframework.stereotype.Component;

@Component
public class PhoneFieldMapper {

    public record MappingResult(List<FactChange> facts, List<Candidate> candidates) {}

    public MappingResult mapCompanyPhone(PhoneValidatorClient.PhoneValidationResult validation,
                                          String currentNormalized, String currentCountry,
                                          String currentRegion, String currentCarrier,
                                          String currentType, String currentStatus) {
        List<FactChange> facts = new ArrayList<>();

        if (validation.e164() != null) {
            facts.add(new FactChange(FactEntity.COMPANY, "primary_phone_normalized",
                    currentNormalized, validation.e164(), false));
        }
        if (validation.countryCode() != null) {
            facts.add(new FactChange(FactEntity.COMPANY, "primary_phone_country",
                    currentCountry, validation.countryCode(), false));
        }
        if (validation.region() != null) {
            facts.add(new FactChange(FactEntity.COMPANY, "primary_phone_region",
                    currentRegion, validation.region(), false));
        }
        if (validation.carrier() != null) {
            facts.add(new FactChange(FactEntity.COMPANY, "primary_phone_carrier",
                    currentCarrier, validation.carrier(), false));
        }
        if (validation.phoneType() != null) {
            facts.add(new FactChange(FactEntity.COMPANY, "primary_phone_type",
                    currentType, validation.phoneType(), false));
        }
        facts.add(new FactChange(FactEntity.COMPANY, "primary_phone_status",
                currentStatus, validation.status(), false));

        return new MappingResult(facts, List.of());
    }

    public MappingResult mapContactPhone(PhoneValidatorClient.PhoneValidationResult validation,
                                           String currentNormalized, String currentCountry,
                                           String currentRegion, String currentCarrier,
                                           String currentType, String currentStatus) {
        List<FactChange> facts = new ArrayList<>();

        if (validation.e164() != null) {
            facts.add(new FactChange(FactEntity.CONTACT, "phone_normalized",
                    currentNormalized, validation.e164(), false));
        }
        if (validation.countryCode() != null) {
            facts.add(new FactChange(FactEntity.CONTACT, "phone_country",
                    currentCountry, validation.countryCode(), false));
        }
        if (validation.region() != null) {
            facts.add(new FactChange(FactEntity.CONTACT, "phone_region",
                    currentRegion, validation.region(), false));
        }
        if (validation.carrier() != null) {
            facts.add(new FactChange(FactEntity.CONTACT, "phone_carrier",
                    currentCarrier, validation.carrier(), false));
        }
        if (validation.phoneType() != null) {
            facts.add(new FactChange(FactEntity.CONTACT, "phone_type",
                    currentType, validation.phoneType(), false));
        }
        facts.add(new FactChange(FactEntity.CONTACT, "phone_status",
                currentStatus, validation.status(), false));

        return new MappingResult(facts, List.of());
    }
}
