package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Borne de date et reste de la règle d'impayé (FEATURE-dossier-durcissement.md §1). */
class ImpayeTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 17);

    @Test
    void echeanceDuJour_tardDansLaJournee_estEchue() {
        assertThat(Impaye.echue(AUJOURDHUI.atTime(23, 59), AUJOURDHUI)).isTrue();
    }

    @Test
    void echeanceDeDemainMinuit_nEstPasEchue() {
        assertThat(Impaye.echue(AUJOURDHUI.plusDays(1).atStartOfDay(), AUJOURDHUI)).isFalse();
    }

    @Test
    void sansDate_jamaisEchue() {
        assertThat(Impaye.echue(null, AUJOURDHUI)).isFalse();
    }

    @Test
    void reste_arrondiAuCentime_regleNulVautZero() {
        assertAll(
                () -> assertThat(Impaye.reste(new BigDecimal("100.004"), null)).isEqualByComparingTo("100.00"),
                () -> assertThat(Impaye.resteDu(new BigDecimal("100"), new BigDecimal("99.996"))).isFalse(),
                () -> assertThat(Impaye.resteDu(new BigDecimal("100"), new BigDecimal("40"))).isTrue());
    }
}
