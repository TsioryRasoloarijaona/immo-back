package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-lot-fiche-parite.md E8, Vérification 9 : la copie écrit des groupes de bordereau ({@code PrjMarketLineGrouping}), connus
 * du rapatriement après le lot qu'ils citent et avant les lignes qui les citent.
 */
class LotsOrdreRemonteeFicheTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    private static int inconnue() {
        return CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size();
    }

    @Test
    void e8_groupeDeBordereau_connu_apresLeLot_avantLesLignes() {
        assertThat(rang("PrjMarketLineGrouping"))
                .isLessThan(inconnue())
                .isGreaterThan(rang("PrjMarket"))
                .isLessThan(rang("PrjMarketLine"));
    }

    @Test
    void e8_ligneDeBordereau_connue_apresLeLot() {
        assertThat(rang("PrjMarketLine")).isLessThan(inconnue()).isGreaterThan(rang("PrjMarket"));
    }
}
