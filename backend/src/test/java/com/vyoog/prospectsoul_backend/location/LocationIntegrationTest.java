package com.vyoog.prospectsoul_backend.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.imports.entity.ImportBatch;
import com.vyoog.prospectsoul_backend.imports.repository.ImportBatchRepository;
import com.vyoog.prospectsoul_backend.imports.repository.ImportRowRepository;
import com.vyoog.prospectsoul_backend.location.dto.response.ExternalPlacesResponse;
import com.vyoog.prospectsoul_backend.location.dto.response.MapCompanyResponse;
import com.vyoog.prospectsoul_backend.location.dto.response.PincodeCentroidResponse;
import com.vyoog.prospectsoul_backend.location.service.CompanyMapService;
import com.vyoog.prospectsoul_backend.location.service.PincodeCentroidService;
import com.vyoog.prospectsoul_backend.location.service.PlacesLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LocationIntegrationTest {

    @Autowired PincodeCentroidService pincodeCentroidService;
    @Autowired CompanyMapService companyMapService;
    @Autowired PlacesLookupService placesLookupService;
    @Autowired CompanyRepository companyRepository;
    @Autowired ImportBatchRepository importBatchRepository;
    @Autowired ImportRowRepository importRowRepository;

    @BeforeEach
    void reset() {
        placesLookupService.resetQuotaCounter();
    }

    @Test
    void pincodeCentroidComesFromSeededTableNotFromGoogle() {
        PincodeCentroidResponse resp = pincodeCentroidService.lookup("641001");
        assertThat(resp.state()).isEqualTo("Tamil Nadu");
        assertThat(resp.centroid().lat()).isEqualByComparingTo(new BigDecimal("11.017100"));
    }

    @Test
    void placesSearchWritesZeroRowsToCompaniesOrImportBatches() {
        long companiesBefore = companyRepository.count();
        long batchesBefore = importBatchRepository.count();
        long rowsBefore = importRowRepository.count();

        ExternalPlacesResponse resp = placesLookupService.search("641001", null, null, null);

        assertThat(resp.persisted()).isFalse();
        assertThat(resp.source()).isEqualTo("stub");
        assertThat(resp.results()).isNotEmpty();
        assertThat(resp.quotaRemaining()).isNotNegative();

        assertThat(companyRepository.count()).isEqualTo(companiesBefore);
        assertThat(importBatchRepository.count()).isEqualTo(batchesBefore);
        assertThat(importRowRepository.count()).isEqualTo(rowsBefore);
    }

    @Test
    void invalidPincodeReturns422() {
        assertThatThrownBy(() -> placesLookupService.search("bad", null, null, null))
                .hasMessageContaining("6 digits");
    }

    @Test
    void mapCompaniesReturnsOwnedRowsWithinRadius() {
        // Coimbatore centroid at (11.0171, 76.9587); place a company inside 2km.
        Company inside = companyRepository.save(Company.builder()
                .canonicalName("Near Company")
                .normalizedName("near company")
                .source("MANUAL")
                .pincode("641001")
                .latitude(new BigDecimal("11.020000"))
                .longitude(new BigDecimal("76.960000"))
                .build());
        Company outside = companyRepository.save(Company.builder()
                .canonicalName("Far Company")
                .normalizedName("far company")
                .source("MANUAL")
                .pincode("110001")
                .latitude(new BigDecimal("28.632700"))
                .longitude(new BigDecimal("77.219700"))
                .build());

        MapCompanyResponse resp = companyMapService.companiesInPincode("641001", 5);
        assertThat(resp.content()).extracting("id").contains(inside.getId());
        assertThat(resp.content()).extracting("id").doesNotContain(outside.getId());
    }

    @Test
    void unknownPincodeThrowsResourceNotFoundWhenGeocoderCannotResolveIt() {
        // The NoOpPincodeGeocoder is wired in tests that don't import
        // TestPincodeGeocoderConfig, so every un-seeded pincode is treated
        // as unknown and the caller sees a proper 404. Previously the
        // service used to return a graceful India-centroid stub; the new
        // resolver rejects unknown pincodes so the user gets an actionable
        // "pincode not found" instead of a plot at (20, 78) they can never
        // interpret.
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> pincodeCentroidService.lookup("603102"))
                .isInstanceOf(com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("603102");
    }

    @Test
    void mapCompaniesForUnseededPincodeStillReturnsMatchingRows() {
        Company c = companyRepository.save(Company.builder()
                .canonicalName("Kanchi Co").normalizedName("kanchi co")
                .source("MANUAL").pincode("603102").build());

        MapCompanyResponse resp = companyMapService.companiesInPincode("603102", 5);
        assertThat(resp.unknownPincode()).isTrue();
        assertThat(resp.content()).extracting("id").contains(c.getId());
    }
}
