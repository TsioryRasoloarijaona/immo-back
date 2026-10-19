package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * FEATURE-patrimoine-ecriture.md §5.5 et §14.1–14.2 : calcul pur des prix d'un lot, intermédiaires en DECIMAL128,
 * valeurs rendues à l'échelle 6 HALF_UP. {@code var} évite de figer l'emplacement du record PrixLot.
 */
class CalculPrixArticleTest {

    private static final BigDecimal TVA_20 = new BigDecimal("0.20");

    private static BigDecimal d(String valeur) {
        return new BigDecimal(valeur);
    }

    @Test
    void r14_1_depuisTTC_casProdArt344_htExact() {
        var lot = CalculPrixArticle.depuisTTC(d("69"), d("13000"), TVA_20);

        assertAll(
                () -> assertThat(lot.prixM2TTC()).isEqualByComparingTo("13000"),
                () -> assertThat(lot.prixM2HT()).isEqualByComparingTo("10833.333333"),
                () -> assertThat(lot.prixTTC()).isEqualByComparingTo("897000"),
                () -> assertThat(lot.prixHT()).isEqualByComparingTo("747500.000000"));
    }

    @Test
    void r14_1_depuisHT_casFictif63m2_ttcArrondiAuStockageSeulement() {
        var lot = CalculPrixArticle.depuisHT(d("63"), d("10833.333333"), TVA_20);

        assertAll(
                () -> assertThat(lot.prixM2HT()).isEqualByComparingTo("10833.333333"),
                () -> assertThat(lot.prixM2TTC()).isEqualByComparingTo("13000.000000"),
                () -> assertThat(lot.prixHT()).isEqualByComparingTo("682499.999979"),
                () -> assertThat(lot.prixTTC()).isEqualByComparingTo("818999.999975"));
    }

    @Test
    void r14_1_depuisTTC_casFictif63m2() {
        var lot = CalculPrixArticle.depuisTTC(d("63"), d("13000"), TVA_20);

        assertAll(
                () -> assertThat(lot.prixTTC()).isEqualByComparingTo("819000"),
                () -> assertThat(lot.prixHT()).isEqualByComparingTo("682500"));
    }

    /** 1 / 1,2 × 3 = 2,5 exactement ; arrondir l'unitaire d'abord donnerait 0,833333 × 3 = 2,499999. */
    @Test
    void r14_1_depuisTTC_unitaireHtNonArrondiAvantMultiplication() {
        var lot = CalculPrixArticle.depuisTTC(d("3"), BigDecimal.ONE, TVA_20);

        assertAll(
                () -> assertThat(lot.prixM2HT()).isEqualByComparingTo("0.833333"),
                () -> assertThat(lot.prixHT()).isEqualByComparingTo("2.500000"),
                () -> assertThat(lot.prixTTC()).isEqualByComparingTo("3"));
    }

    @Test
    void r5_5_tvaZero_htEgalTtc() {
        var lot = CalculPrixArticle.depuisHT(d("50"), d("8000.5"), BigDecimal.ZERO);

        assertAll(
                () -> assertThat(lot.prixM2TTC()).isEqualByComparingTo("8000.5"),
                () -> assertThat(lot.prixHT()).isEqualByComparingTo("400025"),
                () -> assertThat(lot.prixTTC()).isEqualByComparingTo("400025"));
    }

    /** 1,000003 × 1,5 = 1,5000045 → 1,500005 en HALF_UP (HALF_EVEN donnerait 1,500004). */
    @Test
    void r14_2_arrondiHalfUp() {
        var lot = CalculPrixArticle.depuisHT(BigDecimal.ONE, d("1.000003"), d("0.5"));

        assertAll(
                () -> assertThat(lot.prixM2TTC()).isEqualByComparingTo("1.500005"),
                () -> assertThat(lot.prixTTC()).isEqualByComparingTo("1.500005"));
    }

    @Test
    void r14_2_toutesLesValeursAEchelle6() {
        var lot = CalculPrixArticle.depuisTTC(d("69"), d("13000"), TVA_20);

        assertThat(Stream.of(lot.prixM2HT(), lot.prixM2TTC(), lot.prixHT(), lot.prixTTC()).map(BigDecimal::scale).toList())
                .containsExactly(6, 6, 6, 6);
    }

    @Test
    void r14_2_avecSousArticles_sommeLesPrixEtGardeLesPrixAuM2DeLArticle() {
        var article = CalculPrixArticle.depuisTTC(d("63"), d("13000"), TVA_20);
        var parking = CalculPrixArticle.depuisHT(d("10"), d("5000"), TVA_20);
        var jardin = CalculPrixArticle.depuisHT(d("5"), d("1000"), TVA_20);

        var total = CalculPrixArticle.avecSousArticles(article, List.of(parking, jardin));

        assertAll(
                () -> assertThat(total.prixTTC()).isEqualByComparingTo("885000"),
                () -> assertThat(total.prixHT()).isEqualByComparingTo("737500"),
                () -> assertThat(total.prixM2HT()).isEqualByComparingTo(article.prixM2HT()),
                () -> assertThat(total.prixM2TTC()).isEqualByComparingTo(article.prixM2TTC()),
                () -> assertThat(total.prixTTC().scale()).isEqualTo(6));
    }

    @Test
    void r14_2_avecSousArticles_listeVide_prixDeLArticle() {
        var article = CalculPrixArticle.depuisTTC(d("63"), d("13000"), TVA_20);

        var total = CalculPrixArticle.avecSousArticles(article, List.of());

        assertAll(
                () -> assertThat(total.prixTTC()).isEqualByComparingTo("819000"),
                () -> assertThat(total.prixHT()).isEqualByComparingTo("682500"));
    }
}
