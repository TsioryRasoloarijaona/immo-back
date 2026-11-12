package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-achat-fiche-ecritures.md E-B7 : {@code BuyOrderInvoice} (supprimée avec la facture) est déclarée au rapatriement,
 * après {@code BuyDocument} qu'elle cite deux fois, avant {@code PrjWorkingOut}, les règlements et l'audit.
 */
class AchatOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    @Test
    void eb7_buyOrderInvoice_declaree_apresBuyDocument_avantDecomptesReglementsEtAudit() {
        assertThat(rang("BuyOrderInvoice"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(rang("BuyDocument"))
                .isLessThan(rang("PrjWorkingOut"))
                .isLessThan(rang("StlSettlement"))
                .isLessThan(rang("StlSettlementLine"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    /** Les tables déjà portées par les terrains (rang 1100) le restent : une seule déclaration chacune. */
    @Test
    void eb7_tablesDeLaPieceDejaDeclarees_uneSeuleFois() {
        assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES)
                .containsOnlyOnce("BuyDocument", "BuyDocumentLine", "BuyVentilationCategory", "BuyDocumentStatusHistory",
                        "BuyOrderInvoice", "BuyVentilationExpense");
    }
}
