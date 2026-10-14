package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code FEATURE-impayes.md} P1 : forfait {@code k × taux × HT}, arrondi au centime. */
class PenaliteRetardTest {

    private static BigDecimal d(String valeur) {
        return new BigDecimal(valeur);
    }

    @Test
    void sansReglement_kVautUn() {
        assertThat(PenaliteRetard.montantHT(d("0.05"), d("12000"), d("10000"), BigDecimal.ZERO))
                .isEqualByComparingTo("500.00");
    }

    @Test
    void reglementPartiel_kEgalResteSurMontant() {
        assertThat(PenaliteRetard.montantHT(d("0.05"), d("12000"), d("10000"), d("3000")))
                .isEqualByComparingTo("375.00");
    }

    @Test
    void arrondiAuCentime_uneSeuleDivisionEnFin() {
        // k = 2/3 ; 0,1 × 1000 × 2/3 = 66,666… → 66,67 (un k arrondi à 0,67 donnerait 67,00).
        assertThat(PenaliteRetard.montantHT(d("0.1"), d("3"), d("1000"), d("1")))
                .isEqualByComparingTo("66.67")
                .hasScaleOf(2);
    }

    @Test
    void tauxNul_penaliteNulle() {
        assertThat(PenaliteRetard.montantHT(BigDecimal.ZERO, d("12000"), d("10000"), d("3000")))
                .isEqualByComparingTo("0");
    }

    @Test
    void montantNul_kVautUnMalgreLeReglement() {
        assertThat(PenaliteRetard.montantHT(d("0.05"), BigDecimal.ZERO, d("10000"), d("100")))
                .isEqualByComparingTo("500.00");
    }

    @Test
    void valeursNulles_traiteesCommeZero() {
        assertThat(PenaliteRetard.montantHT(null, null, null, null)).isEqualByComparingTo("0");
    }
}
