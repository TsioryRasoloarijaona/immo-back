package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-referentiels.md §5.2 « Pièges » ({@code ORDRE_CLES_ETRANGERES}) : les référentiels écrits par le
 * lot sont connus du rapatriement, en tête (avant {@code CfgShareholder}, {@code CfgTranche} et {@code StkItem} qui les
 * citent) ; les quatre enfants de tranche après {@code CfgTranche} ; tous avant l'audit.
 */
class ReferentielsOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"StkKind", "StkVocation", "LndVocation", "LndLevel", "CfgTrancheStanding", "StkTrancheHeading",
            "StkBuilding", "StkFloor", "StkOrientation", "StkFacade", "StkVue", "StkCostCategory", "StkContenance",
            "StkItemCategory", "StkItemType", "StkExpenseType"})
    void p5_2_referentielEnTete_avantCfgShareholderCfgTrancheStkItemEtAudit(String table) {
        assertThat(rang(table))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isLessThan(rang("CfgShareholder"))
                .isLessThan(rang("CfgTranche"))
                .isLessThan(rang("StkItem"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"StkItemCategory", "StkItemType"})
    void p5_2_categorieEtType_apresLaNature(String table) {
        assertThat(rang(table)).isGreaterThan(rang("StkKind"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"StkPresentationType", "StkPresentationTypeContenance", "StkItemExonaration",
            "CfgTrancheStatus"})
    void p5_2_enfantDeTranche_apresCfgTranche_avantLAudit(String table) {
        assertThat(rang(table))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(rang("CfgTranche"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @Test
    void p5_2_contenanceDePresentation_apresSesDeuxParents() {
        assertThat(rang("StkPresentationTypeContenance"))
                .isGreaterThan(rang("StkPresentationType"))
                .isGreaterThan(rang("StkContenance"));
    }
}
