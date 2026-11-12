package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-lots-liste-parite.md E5, E6, E8, Q7 (écriture sur la base source) : {@code PrjMarket} connue du rapatriement, après
 * ses parents ({@code PrjProject}, {@code PrjMarketType}), avant l'historique d'audit qui la cite ; {@code StrHistory} connue.
 * L'ordre des refus HTTP (403 → 404 → 409) est vérifié dans {@code LotsSuppressionTest}.
 */
class LotsOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    private static int inconnue() {
        return CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size();
    }

    @Test
    void e8_lot_connu_apresProjetEtType_avantLAudit() {
        assertThat(rang("PrjMarket"))
                .isLessThan(inconnue())
                .isGreaterThan(rang("PrjProject"))
                .isGreaterThan(rang("PrjMarketType"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @Test
    void e5_historiqueDeStatut_connu() {
        assertThat(rang("StrHistory")).isLessThan(inconnue());
    }
}
