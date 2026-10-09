package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ConventionRemiseTest {

    @Test
    void pourcentageSaisi_rangeEnRatio_puisRelu_memeTaux() {
        BigDecimal ratio = ConventionRemise.ratioDepuisPourcentage(new BigDecimal("37.5"));

        assertAll(
                () -> assertThat(ratio).isEqualByComparingTo("0.375"),
                () -> assertThat(ConventionRemise.pourcentage(ratio)).isEqualByComparingTo("37.5"));
    }

    @Test
    void ratioDuHT_remiseNegative_ratioNegatif() {
        assertThat(ConventionRemise.ratioDuHT(new BigDecimal("-50000"), new BigDecimal("1000000")))
                .isEqualByComparingTo("-0.05");
    }

    @Test
    void ratioDuHT_baseNulleOuAbsente_zero() {
        assertAll(
                () -> assertThat(ConventionRemise.ratioDuHT(new BigDecimal("100"), BigDecimal.ZERO)).isZero(),
                () -> assertThat(ConventionRemise.ratioDuHT(null, new BigDecimal("100"))).isZero(),
                () -> assertThat(ConventionRemise.ratioDuHT(new BigDecimal("100"), null)).isZero());
    }

    @Test
    void valeursAbsentes_restentAbsentes() {
        assertAll(
                () -> assertThat(ConventionRemise.pourcentage(null)).isNull(),
                () -> assertThat(ConventionRemise.ratioDepuisPourcentage(null)).isNull());
    }
}
