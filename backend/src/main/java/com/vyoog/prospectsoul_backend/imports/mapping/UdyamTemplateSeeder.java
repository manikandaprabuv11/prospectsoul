package com.vyoog.prospectsoul_backend.imports.mapping;

import java.util.List;

import com.vyoog.prospectsoul_backend.imports.entity.ImportTemplate;
import com.vyoog.prospectsoul_backend.imports.entity.ImportTemplateMapping;
import com.vyoog.prospectsoul_backend.imports.repository.ImportTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a default {@code UDYAM_MSME_REGISTRY} column-mapping template on
 * first start. Idempotent — re-runs are a no-op once the template exists.
 *
 * Header names come from the Kanchipuram_data.xlsx analysis (docs/dev_docs/21
 * §9.1). If a source file uses different headers, the analyst can copy the
 * template and adjust it — the mapping suggestion service catches the aliases
 * added in ADR-0008.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UdyamTemplateSeeder implements CommandLineRunner {

    private static final String NAME = "Udyam MSME Registry (default)";
    private static final String SOURCE = "UDYAM_MSME_REGISTRY";

    private final ImportTemplateRepository importTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        boolean already = importTemplateRepository.findAll().stream()
                .anyMatch(t -> NAME.equals(t.getName()));
        if (already) return;

        ImportTemplate t = ImportTemplate.builder()
                .name(NAME)
                .source(SOURCE)
                .isDefault(true)
                .createdBy("system")
                .build();
        List<ImportTemplateMapping> mappings = List.of(
                mapping(t, "LG_ST_Code",           "lg_state_code"),
                mapping(t, "State",                "state"),
                mapping(t, "LG_DT_Code",           "lg_district_code"),
                mapping(t, "District",             "district"),
                mapping(t, "Pincode",              "pincode"),
                mapping(t, "RegistrationDate",     "registration_date"),
                mapping(t, "EnterpriseName",       "canonical_name"),
                mapping(t, "CommunicationAddress", "address_line"),
                mapping(t, "Activities",           "activities_json")
        );
        t.setMappings(mappings);
        importTemplateRepository.save(t);
        log.info("Seeded default import template: {}", NAME);
    }

    private static ImportTemplateMapping mapping(ImportTemplate t, String header, String target) {
        return ImportTemplateMapping.builder()
                .template(t)
                .sourceHeader(header)
                .targetField(target)
                .isActive(true)
                .build();
    }
}
