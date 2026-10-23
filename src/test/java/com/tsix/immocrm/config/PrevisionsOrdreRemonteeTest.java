package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-faisabilite-workflow.md §12.1 ({@code EcrituresFaisabiliteFinance}, rang 1610) : les cinq tables écrites
 * par P8b-1 sont connues du rapatriement, chacune après ses parents, toutes avant l'audit.
 */
class PrevisionsOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"FSFunding", "FSReimbursement", "FSPrevision", "StkFSPrevisionStkTranche", "StkFSPrevisionLine"})
    void r12_tableEcrite_connue_avantLAudit(String table) {
        assertThat(rang(table))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isLessThan(rang("AdmWebHistorics"));
    }

    @Test
    void r12_credit_apresLEtude() {
        assertThat(rang("FSFunding")).isGreaterThan(rang("StkFeasibilityStudy"));
    }

    @Test
    void r12_remboursement_apresSonCredit() {
        assertThat(rang("FSReimbursement")).isGreaterThan(rang("FSFunding"));
    }

    @Test
    void r12_prevision_apresLEtudeEtLaLigneDUnite() {
        assertThat(rang("FSPrevision"))
                .isGreaterThan(rang("StkFeasibilityStudy"))
                .isGreaterThan(rang("StkFSBaseUnitStkFeasibilityStudy"));
    }

    @Test
    void r12_ligneParTranche_apresLaTrancheDEtude() {
        assertThat(rang("StkFSPrevisionStkTranche"))
                .isGreaterThan(rang("StkFeasibilityStudy"))
                .isGreaterThan(rang("StkFeasibilityStudyCfgTranche"));
    }

    @Test
    void r12_ligneDeTresorerie_apresLaTrancheDEtude() {
        assertThat(rang("StkFSPrevisionLine"))
                .isGreaterThan(rang("StkFeasibilityStudy"))
                .isGreaterThan(rang("StkFeasibilityStudyCfgTranche"));
    }
}
