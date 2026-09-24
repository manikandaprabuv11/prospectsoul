package com.vyoog.prospectsoul_backend.enrichment.phone;

import java.math.BigDecimal;
import java.util.*;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.contact.entity.Contact;
import com.vyoog.prospectsoul_backend.contact.repository.ContactRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhoneProvider implements EnrichmentProvider {

    public static final String KEY = "PHONE";

    private final PhoneValidatorClient validatorClient;
    private final PhoneFieldMapper fieldMapper;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.PHONE_VALIDATION, ProviderCapability.COMPANY_ENRICHMENT,
                ProviderCapability.CONTACT_ENRICHMENT);
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        UUID companyId = request.companyId();
        UUID contactId = request.contactId();

        List<FactChange> allFacts = new ArrayList<>();
        List<Candidate> allCandidates = new ArrayList<>();
        List<Map<String, Object>> validationLog = new ArrayList<>();

        if (companyId != null) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

            String phone = company.getPrimaryPhoneNormalized();
            if (phone != null && !phone.isBlank()) {
                PhoneValidatorClient.PhoneValidationResult result = validatorClient.validate(phone);
                PhoneFieldMapper.MappingResult mapping = fieldMapper.mapCompanyPhone(result,
                        company.getPrimaryPhoneNormalized(), company.getPrimaryPhoneCountry(),
                        company.getPrimaryPhoneRegion(), company.getPrimaryPhoneCarrier(),
                        company.getPrimaryPhoneType(), company.getPrimaryPhoneStatus());
                allFacts.addAll(mapping.facts());
                allCandidates.addAll(mapping.candidates());
                validationLog.add(Map.of("entity", "COMPANY", "phone", phone,
                        "status", result.status(), "e164", result.e164() != null ? result.e164() : ""));
            }

            List<Contact> contacts = contactRepository.findByCompanyId(companyId);
            for (Contact contact : contacts) {
                if (contact.getPhone() != null && !contact.getPhone().isBlank()) {
                    PhoneValidatorClient.PhoneValidationResult result = validatorClient.validate(contact.getPhone());
                    PhoneFieldMapper.MappingResult mapping = fieldMapper.mapContactPhone(result,
                            contact.getPhoneNormalized(), contact.getPhoneCountry(),
                            contact.getPhoneRegion(), contact.getPhoneCarrier(),
                            contact.getPhoneType(), contact.getPhoneStatus());
                    allFacts.addAll(mapping.facts());
                    allCandidates.addAll(mapping.candidates());
                    validationLog.add(Map.of("entity", "CONTACT", "contact_id", contact.getId().toString(),
                            "phone", contact.getPhone(), "status", result.status(),
                            "e164", result.e164() != null ? result.e164() : ""));
                }
            }
        }

        String rawPayload;
        try {
            rawPayload = objectMapper.writeValueAsString(Map.of("validations", validationLog));
        } catch (Exception e) {
            rawPayload = "{\"error\": \"serialization_failed\"}";
        }

        return new ProviderResult(
                allFacts.isEmpty() ? ProviderStatus.PARTIAL : ProviderStatus.SUCCESS,
                allFacts, allCandidates, rawPayload,
                BigDecimal.ZERO, null, null
        );
    }
}
