package com.tsix.immocrm.domain.projet;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class MontantsLotTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void choisir_bordereauPresent_lEmporteSurLeForfait() {
        MontantsLot.Trio t = MontantsLot.choisir(new MontantsLot.Trio(d("250.004"), d("50"), d("300.005"), false),
                d("9999"), d("0"), d("9999"));

        assertAll(
                () -> assertThat(t.ht()).isEqualByComparingTo("250.00"),
                () -> assertThat(t.ttc()).isEqualByComparingTo("300.01"),
                () -> assertThat(t.depuisBordereau()).isTrue());
    }

    @Test
    void choisir_sansBordereau_repliSurLeForfait_nulDevientZero() {
        MontantsLot.Trio t = MontantsLot.choisir(null, d("1000"), null, d("1200"));

        assertAll(
                () -> assertThat(t.ht()).isEqualByComparingTo("1000"),
                () -> assertThat(t.tva()).isEqualByComparingTo("0"),
                () -> assertThat(t.depuisBordereau()).isFalse());
    }

    @Test
    void avancement_nonPlafonne_nullSiMarcheNul() {
        assertAll(
                () -> assertThat(MontantsLot.avancementPct(d("600"), d("1200"))).isEqualByComparingTo("50"),
                () -> assertThat(MontantsLot.avancementPct(d("1500"), d("1200"))).isEqualByComparingTo("125"),
                () -> assertThat(MontantsLot.avancementPct(null, d("1200"))).isEqualByComparingTo("0"),
                () -> assertThat(MontantsLot.avancementPct(d("10"), BigDecimal.ZERO)).isNull(),
                () -> assertThat(MontantsLot.avancementPct(d("10"), null)).isNull());
    }

    @Test
    void tauxEtApplication() {
        assertAll(
                () -> assertThat(MontantsLot.applique(d("1200"), d("0.1"))).isEqualByComparingTo("120.00"),
                () -> assertThat(MontantsLot.applique(null, d("0.1"))).isEqualByComparingTo("0"),
                () -> assertThat(MontantsLot.pourcentage(d("0.075"))).isEqualByComparingTo("7.5"),
                () -> assertThat(MontantsLot.pourcentage(null)).isNull());
    }
}
