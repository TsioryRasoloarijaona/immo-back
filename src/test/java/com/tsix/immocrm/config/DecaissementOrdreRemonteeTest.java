package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-saisie-reglements.md D11 : {@code StlSettlementHistory} (écrite à la saisie d'un décaissement) est déclarée
 * au rapatriement, après {@code StlSettlement} qu'elle cite.
 */
class DecaissementOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    @Test
    void d11_historiqueReglement_declare_apresStlSettlement() {
        assertThat(rang("StlSettlementHistory"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(rang("StlSettlement"));
    }

    @Test
    void d11_tablesDuReglement_declareesUneSeuleFois() {
        assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES)
                .containsOnlyOnce("StlSettlement", "StlSettlementLine", "StlSettlementHistory");
    }
}
