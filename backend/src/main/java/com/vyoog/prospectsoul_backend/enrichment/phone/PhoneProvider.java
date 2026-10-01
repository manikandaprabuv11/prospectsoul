package com.vyoog.prospectsoul_backend.enrichment.phone;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.phone.entity.CompanyPhone;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceMode;
import com.vyoog.prospectsoul_backend.company.phone.repository.CompanyPhoneRepository;
import com.vyoog.prospectsoul_backend.company.phone.service.ConfidenceEngine;
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
    private final CompanyPhoneRepository companyPhoneRepository;
    private final ConfidenceEngine confidenceEngine;
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

            // Enrich all company phones in company_phones table
            List<CompanyPhone> phones = companyPhoneRepository
                    .findByCompanyIdOrderByIsPrimaryDescConfidenceAscCreatedAtDesc(companyId);

            for (CompanyPhone phone : phones) {
                String number = phone.getNumberNormalized();
                if (number == null || number.isBlank()) continue;

                PhoneValidatorClient.PhoneValidationResult result = validatorClient.validate(number);

                phone.setEnrichedCountry(result.countryCode());
                phone.setEnrichedRegion(result.region());
                phone.setEnrichedCarrier(result.carrier());
                phone.setEnrichedLineType(result.phoneType());
                phone.setEnrichedStatus(result.status());
                phone.setEnrichedDnd(null);
                phone.setEnrichedAt(Instant.now());

                // Recompute confidence using designation info
                if (phone.getConfidenceMode() != ConfidenceMode.MANUAL) {
                    String designation = phone.resolveDesignation();
                    ConfidenceEngine.ComputeResult confResult = confidenceEngine.compute(
                            phone.getNumberSource(), designation,
                            phone.getConfidence(), phone.getConfidenceMode(), null);
                    ConfidenceLevel newConf = confResult.confidence();

                    // Validated phones get at least MEDIUM confidence
                    if (newConf == ConfidenceLevel.LOW) {
                        newConf = ConfidenceLevel.MEDIUM;
                    }

                    phone.setConfidence(newConf);
                    phone.setConfidenceMode(confResult.mode());
                }

                companyPhoneRepository.save(phone);

                validationLog.add(Map.of("entity", "COMPANY_PHONE", "phone_id", phone.getId().toString(),
                        "phone", number, "status", result.status(),
                        "e164", result.e164() != null ? result.e164() : ""));
            }

            // Fallback: also validate the legacy primary phone on the company
            // if there are no company_phones records
            if (phones.isEmpty()) {
                String legacyPhone = company.getPrimaryPhoneNormalized();
                if (legacyPhone != null && !legacyPhone.isBlank()) {
                    PhoneValidatorClient.PhoneValidationResult result = validatorClient.validate(legacyPhone);
                    PhoneFieldMapper.MappingResult mapping = fieldMapper.mapCompanyPhone(result,
                            legacyPhone, null, null, null, null, null);
                    allFacts.addAll(mapping.facts());
                    validationLog.add(Map.of("entity", "COMPANY_LEGACY", "phone", legacyPhone,
                            "status", result.status(),
                            "e164", result.e164() != null ? result.e164() : ""));
                }
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
                allFacts.isEmpty() && !validationLog.isEmpty() ? ProviderStatus.SUCCESS :
                allFacts.isEmpty() ? ProviderStatus.PARTIAL : ProviderStatus.SUCCESS,
                allFacts, allCandidates, rawPayload,
                BigDecimal.ZERO, null, null
        );
    }
}
