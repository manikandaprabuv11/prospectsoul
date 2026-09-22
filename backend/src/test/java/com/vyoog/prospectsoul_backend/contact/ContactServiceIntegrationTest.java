package com.vyoog.prospectsoul_backend.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.contact.dto.request.ContactCreateRequest;
import com.vyoog.prospectsoul_backend.contact.dto.response.ContactResponse;
import com.vyoog.prospectsoul_backend.contact.repository.ContactRepository;
import com.vyoog.prospectsoul_backend.contact.service.ContactService;
import com.vyoog.prospectsoul_backend.contactrole.repository.ContactRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ContactServiceIntegrationTest {

    @Autowired ContactService contactService;
    @Autowired ContactRepository contactRepository;
    @Autowired ContactRoleRepository contactRoleRepository;
    @Autowired CompanyRepository companyRepository;

    private Company createCompany(String name) {
        return companyRepository.save(Company.builder()
                .canonicalName(name)
                .normalizedName(name.toLowerCase())
                .source("MANUAL_ENTRY")
                .build());
    }

    @Test
    void firstContactIsAutoPromotedToPrimary() {
        Company c = createCompany("Acme");
        var mdRole = contactRoleRepository.findByKey("MD_OWNER").orElseThrow();

        ContactResponse first = contactService.create(c.getId(),
                new ContactCreateRequest("R. Kumar", "MD", "9876543210", null,
                        mdRole.getId(), false, null, null),
                "actor");

        assertThat(first.isPrimary()).isTrue();
        assertThat(first.isMdOwner()).isTrue();
        assertThat(first.roleKey()).isEqualTo("MD_OWNER");
    }

    @Test
    void addingSecondContactAsPrimaryAtomicallyDemotesFirst() {
        Company c = createCompany("Acme 2");
        var mdRole = contactRoleRepository.findByKey("MD_OWNER").orElseThrow();
        var purRole = contactRoleRepository.findByKey("PURCHASE_HEAD").orElseThrow();

        contactService.create(c.getId(),
                new ContactCreateRequest("R. Kumar", null, null, null,
                        mdRole.getId(), true, null, null), "actor");
        ContactResponse second = contactService.create(c.getId(),
                new ContactCreateRequest("S. Raj", null, null, null,
                        purRole.getId(), true, null, null), "actor");

        var rows = contactRepository.findByCompanyIdOrderByIsPrimaryDescCreatedAtAsc(c.getId());
        long primaries = rows.stream().filter(r -> Boolean.TRUE.equals(r.getIsPrimary())).count();
        assertThat(primaries).isEqualTo(1);
        assertThat(rows.stream().filter(r -> r.getId().equals(second.id())).findFirst().orElseThrow().getIsPrimary()).isTrue();
    }

    @Test
    void makePrimarySwapsAtomically() {
        Company c = createCompany("Acme 3");
        var mdRole = contactRoleRepository.findByKey("MD_OWNER").orElseThrow();
        var hrRole = contactRoleRepository.findByKey("HR_HEAD").orElseThrow();

        ContactResponse md = contactService.create(c.getId(),
                new ContactCreateRequest("MD", null, null, null,
                        mdRole.getId(), true, null, null), "actor");
        ContactResponse hr = contactService.create(c.getId(),
                new ContactCreateRequest("HR", null, null, null,
                        hrRole.getId(), false, null, null), "actor");

        contactService.makePrimary(hr.id(), "actor");

        var rows = contactRepository.findByCompanyIdOrderByIsPrimaryDescCreatedAtAsc(c.getId());
        assertThat(rows.stream().filter(r -> Boolean.TRUE.equals(r.getIsPrimary())).count()).isEqualTo(1);
        assertThat(contactRepository.findById(md.id()).orElseThrow().getIsPrimary()).isFalse();
        assertThat(contactRepository.findById(hr.id()).orElseThrow().getIsPrimary()).isTrue();
    }

    @Test
    void creatingWithInactiveRoleReturns422() {
        Company c = createCompany("Acme 4");
        var other = contactRoleRepository.findByKey("OTHER").orElseThrow();
        other.setActive(false);
        contactRoleRepository.saveAndFlush(other);

        assertThatThrownBy(() -> contactService.create(c.getId(),
                new ContactCreateRequest("X", null, null, null, other.getId(),
                        false, null, null), "actor"))
                .hasMessageContaining("inactive");

        // restore for other tests
        other.setActive(true);
        contactRoleRepository.saveAndFlush(other);
    }
}
