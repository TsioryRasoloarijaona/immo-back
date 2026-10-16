package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodiciteTest {

    private static final LocalDateTime DEBUT = LocalDateTime.of(2026, 11, 5, 0, 0);

    @ParameterizedTest
    @CsvSource({"100, 3", "100, 6", "656000, 7", "0.05, 10", "1083600, 24", "10, 4"})
    void laSommeDesEcheancesEstExacteAuCentime(String total, int nombre) {
        List<Periodicite.Echeance> echeances = Periodicite.MENSUELLE.repartir(new BigDecimal(total), nombre, DEBUT);

        assertThat(echeances).isNotEmpty().hasSizeLessThanOrEqualTo(nombre);
        assertThat(echeances.stream().map(Periodicite.Echeance::montant).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(total);
    }

    @Test
    void troisEcheances_laDerniereGardeLeReste() {
        assertThat(Periodicite.MENSUELLE.repartir(new BigDecimal("100"), 3, DEBUT))
                .extracting(Periodicite.Echeance::montant)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
    }

    /** 100 / 6 = 16,67 : la sixième n'a plus que 16,65, elle clôt la série (ComPeriodicity.GenerateLines). */
    @Test
    void periodeArrondieAuDessus_laDerniereEstPlusPetite() {
        assertThat(Periodicite.MENSUELLE.repartir(new BigDecimal("100"), 6, DEBUT))
                .hasSize(6)
                .last()
                .satisfies(e -> assertThat(e.montant()).isEqualByComparingTo("16.65"));
    }

    @Test
    void trimestrielle_datesDeTroisMoisEnTroisMois() {
        assertThat(Periodicite.TRIMESTRIELLE.repartir(new BigDecimal("1200"), 4, DEBUT))
                .extracting(Periodicite.Echeance::date)
                .containsExactly(DEBUT, DEBUT.plusMonths(3), DEBUT.plusMonths(6), DEBUT.plusMonths(9));
    }

    /** AddMonths cumulé : un 31 janvier devient 28 février, puis 28 mars (pas 31). */
    @Test
    void finDeMois_lesDatesSeCumulentCommeOmran() {
        LocalDateTime trenteEtUn = LocalDateTime.of(2027, 1, 31, 0, 0);

        assertThat(Periodicite.MENSUELLE.repartir(new BigDecimal("300"), 3, trenteEtUn))
                .extracting(Periodicite.Echeance::date)
                .containsExactly(trenteEtUn, LocalDateTime.of(2027, 2, 28, 0, 0), LocalDateTime.of(2027, 3, 28, 0, 0));
    }
}
