package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-tranche-onglets.md §8.4, §7 : les 4 tables d'onglets sont connues du rapatriement, après
 * {@code CfgTranche} (parent avant enfant) et avant l'audit qui les cite.
 */
class TrancheOngletsOrdreRemonteeTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"CfgTrancheStkItem", "CfgTrancheLevel", "CfgTrancheCoveredFloor", "CfgTrancheSettlementCategory"})
    void r8_4_ongletConnu_apresLaTranche_avantLAudit(String table) {
        int rang = CollecteurLignesEcrites.rang(table);

        assertThat(rang)
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(CollecteurLignesEcrites.rang("CfgTranche"))
                .isLessThan(CollecteurLignesEcrites.rang("AdmWebHistorics"));
    }

    /** {@code CfgTrancheStkItem.StkItemId} référence le terrain : l'article remonte avant le lien. */
    @Test
    void r8_4_lienTerrain_apresLArticle() {
        assertThat(CollecteurLignesEcrites.rang("CfgTrancheStkItem"))
                .isGreaterThan(CollecteurLignesEcrites.rang("StkItem"));
    }
}
