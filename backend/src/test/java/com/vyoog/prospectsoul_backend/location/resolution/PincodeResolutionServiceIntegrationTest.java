package com.vyoog.prospectsoul_backend.location.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.location.entity.PincodeCentroid;
import com.vyoog.prospectsoul_backend.location.repository.PincodeCentroidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import({TestcontainersConfiguration.class, TestPincodeGeocoderConfig.class})
class PincodeResolutionServiceIntegrationTest {

    @Autowired PincodeResolutionService resolver;
    @Autowired PincodeCentroidRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PincodeGeocoder geocoder;

    private TestPincodeGeocoderConfig.RecordingStub stub() {
        return (TestPincodeGeocoderConfig.RecordingStub) geocoder;
    }

    @BeforeEach
    void reset() {
        jdbc.execute("TRUNCATE company_nic_codes, contacts, import_rows, "
                + "verification_batch_items, activities, companies, pincode_centroids "
                + "RESTART IDENTITY CASCADE");
        stub().fixtures.clear();
        stub().callCount.set(0);
    }

    @Test
    void cachedHitSkipsGeocoderCall() {
        resolver.seed("641006", new BigDecimal("11.017100"), new BigDecimal("76.958700"),
                "R.S. Puram", "Coimbatore", "Tamil Nadu");
        PincodeCentroid result = resolver.resolve("641006");
        assertThat(result.getAreaName()).isEqualTo("R.S. Puram");
        assertThat(stub().callCount.get()).as("geocoder must not be called on cache hit").isZero();
    }

    @Test
    void coldMissCallsGeocoderAndPersistsResult() {
        stub().with("641006", 11.017100, 76.958700, "R.S. Puram", "Coimbatore", "Tamil Nadu");
        PincodeCentroid result = resolver.resolve("641006");
        assertThat(result.getAreaName()).isEqualTo("R.S. Puram");
        assertThat(result.getLatitude()).isEqualByComparingTo(new BigDecimal("11.017100"));
        assertThat(stub().callCount.get()).isEqualTo(1);
        // Persisted?
        PincodeCentroid stored = repository.findByPincode("641006").orElseThrow();
        assertThat(stored.getDistrict()).isEqualTo("Coimbatore");
        // Second call is a cache hit — geocoder count does not increase.
        resolver.resolve("641006");
        assertThat(stub().callCount.get()).as("second call must be a cache hit").isEqualTo(1);
    }

    @Test
    void unknownPincodeThrowsResourceNotFound() {
        assertThatThrownBy(() -> resolver.resolve("999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
        assertThat(repository.findByPincode("999999")).isEmpty();
    }

    @Test
    void invalidPincodeThrowsIllegalArgument() {
        assertThatThrownBy(() -> resolver.resolve("abc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resolver.resolve("12345"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentResolutionOfSamePincodeYieldsExactlyOneRow() throws Exception {
        stub().with("560001", 12.976700, 77.590100, "Bangalore G.P.O.", "Bangalore", "Karnataka");

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CompletableFuture<?>[] futures = new CompletableFuture[8];
            for (int i = 0; i < 8; i++) {
                futures[i] = CompletableFuture.runAsync(() -> resolver.resolve("560001"), pool);
            }
            CompletableFuture.allOf(futures).get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdown();
        }

        // Exactly one row (PK-enforced) and one canonical value.
        long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pincode_centroids WHERE pincode = ?", Long.class, "560001");
        assertThat(count).isEqualTo(1L);
        PincodeCentroid stored = repository.findByPincode("560001").orElseThrow();
        assertThat(stored.getAreaName()).isEqualTo("Bangalore G.P.O.");
    }

    @Test
    void peekNeverCallsGeocoderAndReturnsEmptyWhenNotCached() {
        assertThat(resolver.peek("641006")).isEmpty();
        assertThat(stub().callCount.get()).isZero();
    }

    @Test
    void refreshUpgradesAnExistingRowInPlace() {
        resolver.seed("641006", new BigDecimal("20.593700"), new BigDecimal("78.962900"),
                "Auto-seeded", null, null);
        stub().with("641006", 11.017100, 76.958700, "R.S. Puram", "Coimbatore", "Tamil Nadu");
        PincodeCentroid refreshed = resolver.refresh("641006");
        assertThat(refreshed.getAreaName()).isEqualTo("R.S. Puram");
        assertThat(refreshed.getDistrict()).isEqualTo("Coimbatore");
        long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pincode_centroids WHERE pincode = ?", Long.class, "641006");
        assertThat(count).isEqualTo(1L);
    }
}
