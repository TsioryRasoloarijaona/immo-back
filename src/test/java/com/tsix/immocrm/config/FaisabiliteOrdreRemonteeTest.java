package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-faisabilite.md §7.1 ({@code CollecteurLignesEcrites}) et §8 bis décision 3 : les tables écrites par
 * P8a sont connues du rapatriement, chacune après ses parents, toutes avant l'audit.
 */
class FaisabiliteOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"SpecificFunction", "StkFSBaseUnit", "StkFeasibilityStudy", "StkFSBaseUnitStkFeasibilityStudy",
            "StkFeasibilityStudyCfgTranche", "StkFeasibilityStudyCfgTrancheStkBaseUnit", "StkFeasibilityStudyStatusHistory"})
    void p7_1_tableEcrite_connue_avantLAudit(String table) {
        assertThat(rang(table))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isLessThan(rang("AdmWebHistorics"));
    }

    @Test
    void p7_1_uniteDeBase_apresSaFonctionEtSaCategorie_apresStkExpenseType() {
        assertThat(rang("StkFSBaseUnit"))
                .isGreaterThan(rang("SpecificFunction"))
                .isGreaterThan(rang("StkItemCategory"))
                .isGreaterThan(rang("StkExpenseType"));
    }

    @Test
    void p7_1_etude_apresSocieteArticleEtTranche() {
        assertThat(rang("StkFeasibilityStudy"))
                .isGreaterThan(rang("CfgCompany"))
                .isGreaterThan(rang("StkItem"))
                .isGreaterThan(rang("CfgTrancheStkItem"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @Test
    void p7_1_ligneDUnite_apresLEtudeEtLUniteDeBase() {
        assertThat(rang("StkFSBaseUnitStkFeasibilityStudy"))
                .isGreaterThan(rang("StkFeasibilityStudy"))
                .isGreaterThan(rang("StkFSBaseUnit"));
    }

    @Test
    void p7_1_trancheDEtude_apresLEtudeEtCfgTranche() {
        assertThat(rang("StkFeasibilityStudyCfgTranche"))
                .isGreaterThan(rang("StkFeasibilityStudy"))
                .isGreaterThan(rang("CfgTranche"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @Test
    void p7_1_ventilation_apresLaTrancheDEtudeEtLaLigneDUnite() {
        assertThat(rang("StkFeasibilityStudyCfgTrancheStkBaseUnit"))
                .isGreaterThan(rang("StkFeasibilityStudyCfgTranche"))
                .isGreaterThan(rang("StkFSBaseUnitStkFeasibilityStudy"));
    }

    @Test
    void d8bis3_historique_apresLEtude() {
        assertThat(rang("StkFeasibilityStudyStatusHistory")).isGreaterThan(rang("StkFeasibilityStudy"));
    }
}
