package com.vyoog.prospectsoul_backend.verification.provider;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The subset of the Twilio Lookup v2 response this module reads. Unknown
 * properties are ignored so a provider-side additive change cannot break a
 * running batch.
 *
 * <p>Twilio's Lookup v2 payload is snake_case regardless of this application's
 * own JSON convention, so every field is mapped explicitly rather than relying
 * on the global naming strategy.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwilioLookupResponse(
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("country_code") String countryCode,
        @JsonProperty("calling_country_code") String callingCountryCode,
        @JsonProperty("valid") Boolean valid,
        @JsonProperty("validation_errors") List<String> validationErrors,
        @JsonProperty("line_type_intelligence") LineTypeIntelligence lineTypeIntelligence,
        @JsonProperty("url") String url
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineTypeIntelligence(
            @JsonProperty("type") String type,
            @JsonProperty("carrier_name") String carrierName,
            @JsonProperty("mobile_country_code") String mobileCountryCode,
            @JsonProperty("mobile_network_code") String mobileNetworkCode,
            @JsonProperty("error_code") Integer errorCode
    ) {}
}
