package com.vyoog.prospectsoul_backend.enrichment.google;

import java.util.ArrayList;
import java.util.List;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.Candidate;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.FactChange;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.FactChange.FactEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class GooglePlacesFieldMapper {

    public record MappingResult(List<FactChange> facts, List<Candidate> candidates) {}

    public MappingResult mapFields(JsonNode place, Company company) {
        List<FactChange> facts = new ArrayList<>();
        List<Candidate> candidates = new ArrayList<>();

        addFact(facts, "google_place_id", company.getGooglePlaceId(), textValue(place, "id"), false);

        JsonNode displayName = place.get("displayName");
        if (displayName != null && displayName.has("text")) {
            addFact(facts, "google_name", company.getGoogleName(), displayName.get("text").asText(), false);
        }

        String formattedAddress = textValue(place, "formattedAddress");
        if (formattedAddress != null) {
            if (company.getAddressLine() == null || company.getAddressLine().isBlank()) {
                addFact(facts, "address_line", company.getAddressLine(), formattedAddress, false);
            } else {
                candidates.add(new Candidate("FIELD_OVERWRITE", "address_line",
                        formattedAddress, company.getAddressLine()));
            }
        }

        String phone = textValue(place, "internationalPhoneNumber");
        if (phone != null) {
            if (company.getPrimaryPhoneNormalized() == null || company.getPrimaryPhoneNormalized().isBlank()) {
                addFact(facts, "primary_phone_normalized", company.getPrimaryPhoneNormalized(), phone, false);
            } else {
                candidates.add(new Candidate("PHONE_NEW_CONTACT", "primary_phone_normalized",
                        phone, company.getPrimaryPhoneNormalized()));
            }
        }

        String websiteUri = textValue(place, "websiteUri");
        if (websiteUri != null) {
            if (company.getWebsiteDomain() == null || company.getWebsiteDomain().isBlank()) {
                addFact(facts, "website_domain", company.getWebsiteDomain(), websiteUri, false);
            } else {
                candidates.add(new Candidate("FIELD_OVERWRITE", "website_domain",
                        websiteUri, company.getWebsiteDomain()));
            }
        }

        JsonNode types = place.get("types");
        if (types != null && types.isArray() && !types.isEmpty()) {
            List<String> typesList = new ArrayList<>();
            types.forEach(t -> typesList.add(t.asText()));
            addFact(facts, "google_business_types", company.getGoogleBusinessTypes(),
                    String.join(",", typesList), false);
            addFact(facts, "google_business_category", company.getGoogleBusinessCategory(),
                    typesList.getFirst(), false);
        }

        addFact(facts, "google_maps_url", company.getGoogleMapsUrl(), textValue(place, "googleMapsUri"), false);

        JsonNode location = place.get("location");
        if (location != null) {
            if (location.has("latitude")) {
                addFact(facts, "google_lat", company.getGoogleLat() != null ? company.getGoogleLat().toPlainString() : null,
                        String.valueOf(location.get("latitude").asDouble()), false);
            }
            if (location.has("longitude")) {
                addFact(facts, "google_lng", company.getGoogleLng() != null ? company.getGoogleLng().toPlainString() : null,
                        String.valueOf(location.get("longitude").asDouble()), false);
            }
        }

        addFact(facts, "google_business_status", company.getGoogleBusinessStatus(),
                textValue(place, "businessStatus"), false);

        return new MappingResult(facts, candidates);
    }

    private void addFact(List<FactChange> facts, String field, String oldValue, String newValue,
                         boolean overwritesHuman) {
        if (newValue != null && !newValue.isBlank()) {
            facts.add(new FactChange(FactEntity.COMPANY, field, oldValue, newValue, overwritesHuman));
        }
    }

    private String textValue(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) return null;
        String text = child.asText();
        return text.isBlank() ? null : text;
    }
}
