package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prédicat d'exclusion des lignes hors périmètre (achat/location). Logique pure, testable sans
 * base ni contexte Spring — {@link RemonteeLecture#predicatPerimetre} est statique.
 */
class RemonteePerimetreTest {

    @Test
    void stlSettlementLine_exclutLesTroisFkHorsPerimetre() {
        assertThat(RemonteeLecture.predicatPerimetre("StlSettlementLine"))
                .isEqualTo("[BuyDocumentId] IS NULL AND [RntDocumentId] IS NULL AND [StlDocumentId] IS NULL");
    }

    @Test
    void tableSansExclusion_renvoieNull() {
        assertThat(RemonteeLecture.predicatPerimetre("CfgTier")).isNull();
        assertThat(RemonteeLecture.predicatPerimetre("ComProspection")).isNull();
        assertThat(RemonteeLecture.predicatPerimetre("StlSettlement")).isNull();
    }
}
