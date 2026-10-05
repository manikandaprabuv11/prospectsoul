package com.vyoog.prospectsoul_backend.company.phone;

import com.vyoog.prospectsoul_backend.admin.designation.service.DecisionMakerDesignationService;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceMode;
import com.vyoog.prospectsoul_backend.company.phone.entity.NumberSourceType;
import com.vyoog.prospectsoul_backend.company.phone.service.ConfidenceEngine;
import com.vyoog.prospectsoul_backend.company.phone.service.ConfidenceEngine.ComputeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfidenceEngineTest {

    @Mock
    private DecisionMakerDesignationService designationService;

    private ConfidenceEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ConfidenceEngine(designationService);
    }

    private ComputeResult compute(NumberSourceType source, String designation,
                                   ConfidenceLevel existingConf, ConfidenceMode existingMode,
                                   ConfidenceLevel userSelected) {
        return engine.compute(source, designation, existingConf, existingMode, userSelected);
    }

    @Nested
    @DisplayName("TC-1.1: Source-to-Confidence Mapping")
    class SourceMapping {

        @Test
        void businessCard_mapsToHigh() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.BUSINESS_CARD, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.AUTO);
        }

        @Test
        void fieldVisit_mapsToHigh() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.FIELD_VISIT, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.AUTO);
        }

        @Test
        void reference_mapsToHigh() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.REFERENCE, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.AUTO);
        }

        @Test
        void website_mapsToMedium() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.WEBSITE, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.AUTO);
        }

        @Test
        void googleApi_mapsToMedium() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.GOOGLE_API, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.AUTO);
        }

        @Test
        void linkedin_mapsToMedium() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.LINKEDIN, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.AUTO);
        }

        @Test
        void indiamart_mapsToLow() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.INDIAMART, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.LOW);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.AUTO);
        }

        @Test
        void importDefault_mapsToMedium() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.IMPORT_DEFAULT, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.AUTO);
        }

        @Test
        void manualEntry_userSelectsHigh_modeIsManual() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.MANUAL_ENTRY, null, null, null, ConfidenceLevel.HIGH);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.MANUAL);
        }

        @Test
        void manualEntry_userSelectsLow_modeIsManual() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.MANUAL_ENTRY, null, null, null, ConfidenceLevel.LOW);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.LOW);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.MANUAL);
        }

        @Test
        void manualEntry_noSelection_defaultsMediumAuto() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.MANUAL_ENTRY, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.AUTO);
        }
    }

    @Nested
    @DisplayName("TC-1.2: Designation Override — Standard Sources")
    class DesignationOverrideStandard {

        @Test
        void website_md_upgradesToHigh() {
            when(designationService.isDecisionMaker("MD")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.WEBSITE, "MD", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(r.isDecisionMaker()).isTrue();
        }

        @Test
        void googleApi_ceo_upgradesToHigh() {
            when(designationService.isDecisionMaker("CEO")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.GOOGLE_API, "CEO", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(r.isDecisionMaker()).isTrue();
        }

        @Test
        void linkedin_owner_upgradesToHigh() {
            when(designationService.isDecisionMaker("Owner")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.LINKEDIN, "Owner", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(r.isDecisionMaker()).isTrue();
        }

        @Test
        void importDefault_generalManager_upgradesToHigh() {
            when(designationService.isDecisionMaker("General Manager")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.IMPORT_DEFAULT, "General Manager", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void website_nonDm_staysMedium() {
            when(designationService.isDecisionMaker("Accounts Manager")).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.WEBSITE, "Accounts Manager", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(r.isDecisionMaker()).isFalse();
        }

        @Test
        void website_storeKeeper_staysMedium() {
            when(designationService.isDecisionMaker("Store Keeper")).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.WEBSITE, "Store Keeper", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        void importDefault_purchaseManager_upgradesToHigh() {
            when(designationService.isDecisionMaker("Purchase Manager")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.IMPORT_DEFAULT, "Purchase Manager", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void businessCard_ceo_staysHigh() {
            when(designationService.isDecisionMaker("CEO")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.BUSINESS_CARD, "CEO", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void reference_noDesignation_staysHigh() {
            when(designationService.isDecisionMaker(isNull())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.REFERENCE, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("TC-1.3: Designation Override — IndiaMART Exception")
    class DesignationOverrideIndiamart {

        @Test
        void indiamart_md_upgradesToMediumNotHigh() {
            when(designationService.isDecisionMaker("MD")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.INDIAMART, "MD", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        void indiamart_ceo_upgradesToMedium() {
            when(designationService.isDecisionMaker("CEO")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.INDIAMART, "CEO", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        void indiamart_owner_upgradesToMedium() {
            when(designationService.isDecisionMaker("Owner")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.INDIAMART, "Owner", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        void indiamart_nonDm_staysLow() {
            when(designationService.isDecisionMaker("Accounts Manager")).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.INDIAMART, "Accounts Manager", null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.LOW);
        }

        @Test
        void indiamart_noDesignation_staysLow() {
            when(designationService.isDecisionMaker(isNull())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.INDIAMART, null, null, null, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.LOW);
        }
    }

    @Nested
    @DisplayName("TC-1.4: Never-Downgrade Rule")
    class NeverDowngrade {

        @Test
        void high_indiamart_staysHigh() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.INDIAMART, null, ConfidenceLevel.HIGH, ConfidenceMode.AUTO, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void high_website_staysHigh() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.WEBSITE, null, ConfidenceLevel.HIGH, ConfidenceMode.AUTO, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void medium_indiamart_staysMedium() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.INDIAMART, null, ConfidenceLevel.MEDIUM, ConfidenceMode.AUTO, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        void high_manual_website_md_staysManual() {
            when(designationService.isDecisionMaker("MD")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.WEBSITE, "MD", ConfidenceLevel.HIGH, ConfidenceMode.MANUAL, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(r.mode()).isEqualTo(ConfidenceMode.MANUAL);
        }

        @Test
        void low_website_md_upgradestoHigh() {
            when(designationService.isDecisionMaker("MD")).thenReturn(true);
            ComputeResult r = compute(NumberSourceType.WEBSITE, "MD", ConfidenceLevel.LOW, ConfidenceMode.AUTO, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void medium_businessCard_upgradesToHigh() {
            when(designationService.isDecisionMaker(any())).thenReturn(false);
            ComputeResult r = compute(NumberSourceType.BUSINESS_CARD, null, ConfidenceLevel.MEDIUM, ConfidenceMode.AUTO, null);
            assertThat(r.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("ConfidenceLevel.max()")
    class MaxOrdering {

        @Test
        void high_beats_medium() {
            assertThat(ConfidenceLevel.max(ConfidenceLevel.HIGH, ConfidenceLevel.MEDIUM))
                    .isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void high_beats_low() {
            assertThat(ConfidenceLevel.max(ConfidenceLevel.HIGH, ConfidenceLevel.LOW))
                    .isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void medium_beats_low() {
            assertThat(ConfidenceLevel.max(ConfidenceLevel.MEDIUM, ConfidenceLevel.LOW))
                    .isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        void commutative() {
            assertThat(ConfidenceLevel.max(ConfidenceLevel.LOW, ConfidenceLevel.HIGH))
                    .isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void nullSafe() {
            assertThat(ConfidenceLevel.max(null, ConfidenceLevel.LOW))
                    .isEqualTo(ConfidenceLevel.LOW);
            assertThat(ConfidenceLevel.max(ConfidenceLevel.HIGH, null))
                    .isEqualTo(ConfidenceLevel.HIGH);
        }
    }
}
