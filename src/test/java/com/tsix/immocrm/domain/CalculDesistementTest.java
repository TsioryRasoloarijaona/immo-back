package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CalculDesistementTest {

    private static final CalculDesistement.Entree STANDARD = new CalculDesistement.Entree(new BigDecimal("0.10"), true,
            new BigDecimal("1000000"), new BigDecimal("300000"), BigDecimal.ZERO, new BigDecimal("20000"));

    @Test
    void penaliteSaisie_remiseRecalculee_redonneLaMemePenalite() {
        BigDecimal remise = CalculDesistement.remisePour(new BigDecimal("75000"), STANDARD);

        assertAll(
                () -> assertThat(remise).isEqualByComparingTo("0.25"),
                () -> assertThat(CalculDesistement.penaliteApresRemise(STANDARD.avecRemise(remise)))
                        .isEqualByComparingTo("75000"));
    }

    @Test
    void tauxNul_remiseNulle_etARembourserSansPenalite() {
        CalculDesistement.Entree sansTaux = new CalculDesistement.Entree(BigDecimal.ZERO, true, new BigDecimal("1000000"),
                new BigDecimal("300000"), BigDecimal.ZERO, new BigDecimal("20000"));

        assertAll(
                () -> assertThat(CalculDesistement.remisePour(new BigDecimal("10"), sansTaux)).isEqualByComparingTo("0"),
                () -> assertThat(CalculDesistement.aRembourser(sansTaux)).isEqualByComparingTo("280000"));
    }

    @Test
    void penalite_arrondieAuCentime() {
        CalculDesistement.Entree e = new CalculDesistement.Entree(new BigDecimal("0.15"), false, null,
                new BigDecimal("1234.567"), BigDecimal.ZERO, null);

        assertThat(CalculDesistement.penaliteApresRemise(e)).isEqualByComparingTo("185.19");
    }
}
