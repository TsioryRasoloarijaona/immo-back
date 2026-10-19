package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-tranche-regles.md §7, §9.9 : les 3 tables du lot sont connues du rapatriement, après
 * {@code CfgTranche} (parent avant enfant) et avant l'audit ; {@code ComSalesCommission} après {@code ComAction}
 * (qu'elle référence) et avant {@code ComProspection}.
 */
class TrancheReglesOrdreRemonteeTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"CfgTrancheFolderEntity", "CfgPrevision", "ComSalesCommission"})
    void r9_9_tableConnue_apresLaTranche_avantLAudit(String table) {
        int rang = CollecteurLignesEcrites.rang(table);

        assertThat(rang)
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(CollecteurLignesEcrites.rang("CfgTranche"))
                .isLessThan(CollecteurLignesEcrites.rang("AdmMessage"))
                .isLessThan(CollecteurLignesEcrites.rang("AdmWebHistorics"));
    }

    @Test
    void r9_9_commission_apresLActionEtLeTiers_avantLaProspection() {
        assertThat(CollecteurLignesEcrites.rang("ComSalesCommission"))
                .isGreaterThan(CollecteurLignesEcrites.rang("ComAction"))
                .isGreaterThan(CollecteurLignesEcrites.rang("CfgTier"))
                .isLessThan(CollecteurLignesEcrites.rang("ComProspection"));
    }
}
