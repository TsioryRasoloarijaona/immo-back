package com.tsix.immocrm.domain.tresorerie;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionReglementTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 18);

    @ParameterizedTest(name = "base {0} × {1}/{2} − {3} = {4}")
    @CsvSource(nullValues = "null", value = {
            "1000, 1, 1, 500, 500",
            "100, 2, 1, 50, 150",
            "100, 1, 0, 0, 100",
            "100, null, null, null, 100",
            "null, 1, 1, 30, -30"
    })
    void resteAAffecter_baseConvertieMoinsAffecte(BigDecimal base, BigDecimal valeur, Integer unite, BigDecimal affecte,
                                                  BigDecimal attendu) {
        assertThat(ConversionReglement.resteAAffecter(base, valeur, unite, affecte)).isEqualByComparingTo(attendu);
    }

    @ParameterizedTest(name = "échéance {0}, statut {1} → {2}")
    @CsvSource(nullValues = "null", value = {
            "2026-11-17, IN_WORK, true",
            "2026-11-18, null, true",
            "2026-11-18, POINTED, false",
            "2026-11-19, IN_WORK, false",
            "null, IN_WORK, false"
    })
    void enRetard_echeanceAtteinteEtNonPointe(LocalDate echeance, String statut, boolean attendu) {
        assertThat(ConversionReglement.enRetard(echeance, statut, AUJOURDHUI)).isEqualTo(attendu);
    }
}
