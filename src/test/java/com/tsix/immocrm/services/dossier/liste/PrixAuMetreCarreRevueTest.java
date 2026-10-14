package com.tsix.immocrm.services.dossier.liste;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code ComFolderView.UnitCostTTC = ItemPriceAfter / IIF(Area = 0, 1, Area)} : Area NULL rend NULL en SQL
 * (21 dossiers sur 395 dans yesRemsProd, ex. APF 2026/00381 : vue NULL, liste 2 502 000).
 */
class PrixAuMetreCarreRevueTest {

    @Test
    void prixAuM2_superficieAbsente_nulCommeLaVue() {
        BigDecimal prix = MappingListeDossier.PrixAuMetreCarre.article(new BigDecimal("2502000"), null);

        assertThat(prix).isNull();
    }

    @Test
    void prixAuM2_superficieZero_prixEntierCommeLaVue() {
        BigDecimal prix = MappingListeDossier.PrixAuMetreCarre.article(new BigDecimal("2502000"), BigDecimal.ZERO);

        assertThat(prix).isEqualByComparingTo("2502000");
    }
}
