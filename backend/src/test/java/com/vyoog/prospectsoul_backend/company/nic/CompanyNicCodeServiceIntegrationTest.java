package com.vyoog.prospectsoul_backend.company.nic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.nic.dto.request.AttachNicCodeRequest;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository;
import com.vyoog.prospectsoul_backend.company.nic.service.CompanyNicCodeService;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CompanyNicCodeServiceIntegrationTest {

    @Autowired CompanyNicCodeService service;
    @Autowired CompanyNicCodeRepository joinRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired NicCodeRepository nicCodeRepository;

    private Company company() {
        return companyRepository.save(Company.builder()
                .canonicalName("MultiNic Co").normalizedName("multinic co")
                .source("MANUAL_ENTRY").build());
    }

    private NicCode nic(String code) {
        return nicCodeRepository.save(NicCode.builder()
                .code(code).description("Test " + code).industryType("Manufacturing")
                .level((short) code.length()).active(true).isPrimary(false).build());
    }

    @Test
    void attachingResolvedCodePromotesToPrimaryOnFirstAttach() {
        Company c = company();
        NicCode n = nic("28132");
        var resp = service.attach(c.getId(),
                new AttachNicCodeRequest(n.getId(), null, null, false), "actor");
        assertThat(resp.isPrimary()).isTrue();
        assertThat(resp.nicCodeId()).isEqualTo(n.getId());
        assertThat(companyRepository.findById(c.getId()).orElseThrow().getPrimaryNicCodeId())
                .isEqualTo(n.getId());
    }

    @Test
    void makePrimaryAtomicallyDemotesOthersAndUpdatesDenorm() {
        Company c = company();
        NicCode n1 = nic("2201");
        NicCode n2 = nic("2202");
        var r1 = service.attach(c.getId(), new AttachNicCodeRequest(n1.getId(), null, null, true), "actor");
        var r2 = service.attach(c.getId(), new AttachNicCodeRequest(n2.getId(), null, null, false), "actor");

        service.makePrimary(c.getId(), r2.id(), "actor");

        long primaries = joinRepository.findByCompanyIdOrderBySequenceNoAsc(c.getId()).stream()
                .filter(x -> Boolean.TRUE.equals(x.getIsPrimary())).count();
        assertThat(primaries).isEqualTo(1L);
        assertThat(joinRepository.findById(r2.id()).orElseThrow().getIsPrimary()).isTrue();
        assertThat(joinRepository.findById(r1.id()).orElseThrow().getIsPrimary()).isFalse();
        assertThat(companyRepository.findById(c.getId()).orElseThrow().getPrimaryNicCodeId())
                .isEqualTo(n2.getId());
    }

    @Test
    void attachingUnresolvedRawCodeKeepsRawAndLeavesNicCodeIdNull() {
        Company c = company();
        var resp = service.attach(c.getId(),
                new AttachNicCodeRequest(null, "99999", "unknown activity", false), "actor");
        assertThat(resp.nicCodeId()).isNull();
        assertThat(resp.nicCodeRaw()).isEqualTo("99999");
        assertThat(resp.isPrimary()).isTrue();
        assertThat(companyRepository.findById(c.getId()).orElseThrow().getPrimaryNicCodeId()).isNull();
    }

    @Test
    void detachingPrimaryPromotesEarliestSurvivor() {
        Company c = company();
        NicCode n1 = nic("3101");
        NicCode n2 = nic("3102");
        var r1 = service.attach(c.getId(), new AttachNicCodeRequest(n1.getId(), null, null, true), "actor");
        service.attach(c.getId(), new AttachNicCodeRequest(n2.getId(), null, null, false), "actor");

        service.detach(c.getId(), r1.id(), "actor");
        assertThat(joinRepository.countByCompanyId(c.getId())).isEqualTo(1L);
        assertThat(joinRepository.findByCompanyIdOrderBySequenceNoAsc(c.getId()).getFirst().getIsPrimary()).isTrue();
        assertThat(companyRepository.findById(c.getId()).orElseThrow().getPrimaryNicCodeId())
                .isEqualTo(n2.getId());
    }

    @Test
    void duplicateAttachReturns409() {
        Company c = company();
        NicCode n = nic("4201");
        service.attach(c.getId(), new AttachNicCodeRequest(n.getId(), null, null, false), "actor");
        assertThatThrownBy(() ->
                service.attach(c.getId(), new AttachNicCodeRequest(n.getId(), null, null, false), "actor"))
                .hasMessageContaining("already attached");
    }
}
