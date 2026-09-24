package com.vyoog.prospectsoul_backend.enrichment.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PhoneValidatorClient {

    private static final String DEFAULT_REGION = "IN";

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private final PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
    private final PhoneNumberToCarrierMapper carrierMapper = PhoneNumberToCarrierMapper.getInstance();

    public PhoneValidationResult validate(String phoneNumber) {
        try {
            Phonenumber.PhoneNumber parsed = phoneUtil.parse(phoneNumber, DEFAULT_REGION);
            boolean valid = phoneUtil.isValidNumber(parsed);

            String e164 = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
            int countryCode = parsed.getCountryCode();
            String region = geocoder.getDescriptionForNumber(parsed, Locale.ENGLISH);
            String carrier = carrierMapper.getNameForNumber(parsed, Locale.ENGLISH);
            PhoneNumberUtil.PhoneNumberType type = phoneUtil.getNumberType(parsed);

            return new PhoneValidationResult(
                    true,
                    valid ? "VALID" : "INVALID",
                    e164,
                    String.valueOf(countryCode),
                    region.isBlank() ? null : region,
                    carrier.isBlank() ? null : carrier,
                    mapPhoneType(type),
                    null
            );
        } catch (NumberParseException e) {
            return new PhoneValidationResult(false, "INVALID", null, null, null, null, null, e.getMessage());
        }
    }

    private String mapPhoneType(PhoneNumberUtil.PhoneNumberType type) {
        return switch (type) {
            case FIXED_LINE -> "FIXED_LINE";
            case MOBILE -> "MOBILE";
            case FIXED_LINE_OR_MOBILE -> "FIXED_LINE_OR_MOBILE";
            case TOLL_FREE -> "TOLL_FREE";
            case PREMIUM_RATE -> "PREMIUM_RATE";
            case VOIP -> "VOIP";
            default -> "UNKNOWN";
        };
    }

    public record PhoneValidationResult(
            boolean parseable,
            String status,
            String e164,
            String countryCode,
            String region,
            String carrier,
            String phoneType,
            String error
    ) {}
}
