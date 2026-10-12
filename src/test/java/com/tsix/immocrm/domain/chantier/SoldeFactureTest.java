package com.tsix.immocrm.domain.chantier;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class SoldeFactureTest {

    @Test
    void factureSoldee_resteNul() {
        SoldeFacture s = SoldeFacture.de(new BigDecimal("216"), new BigDecimal("216"));

        assertAll(
                () -> assertThat(s.totalFacture()).isEqualByComparingTo("216"),
                () -> assertThat(s.totalRegle()).isEqualByComparingTo("216"),
                () -> assertThat(s.resteARegler()).isEqualByComparingTo("0"));
    }

    @Test
    void factureSansReglement_regleNullVautZero_resteEgalAuNet() {
        SoldeFacture s = SoldeFacture.de(new BigDecimal("130"), null);

        assertAll(
                () -> assertThat(s.totalRegle()).isEqualByComparingTo("0"),
                () -> assertThat(s.resteARegler()).isEqualByComparingTo("130"));
    }

    @Test
    void sommeDeFactures_sommeDesSoldesUnitaires() {
        SoldeFacture total = SoldeFacture.somme(List.of(
                SoldeFacture.de(new BigDecimal("216"), new BigDecimal("216")),
                SoldeFacture.de(new BigDecimal("130"), null)));

        assertAll(
                () -> assertThat(total.totalFacture()).isEqualByComparingTo("346"),
                () -> assertThat(total.totalRegle()).isEqualByComparingTo("216"),
                () -> assertThat(total.resteARegler()).isEqualByComparingTo("130"));
    }

    @Test
    void aucuneFacture_toutAZero() {
        assertThat(SoldeFacture.somme(List.of())).isEqualTo(SoldeFacture.AUCUNE);
    }
}
