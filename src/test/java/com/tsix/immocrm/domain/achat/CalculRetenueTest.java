package com.tsix.immocrm.domain.achat;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CalculRetenueTest {

    private static final BigDecimal BASE = new BigDecimal("10000.00");

    @Test
    void parTaux_montantEtNetDeduits() {
        BigDecimal rs = CalculRetenue.montantRs(BASE, new BigDecimal("0.10"));

        assertAll(
                () -> assertThat(rs).isEqualByComparingTo("1000.00"),
                () -> assertThat(CalculRetenue.net(BASE, rs)).isEqualByComparingTo("9000.00"));
    }

    @Test
    void parMontant_tauxDeduit() {
        assertThat(CalculRetenue.taux(BASE, new BigDecimal("750.00"))).isEqualByComparingTo("0.075");
    }

    @Test
    void deuxLignes_retenueDuReglementEstLaSommeEtSonTaux() {
        BigDecimal ligne1 = CalculRetenue.montantRs(BASE, new BigDecimal("0.10"));
        BigDecimal ligne2 = CalculRetenue.montantRs(BASE, new BigDecimal("0.05"));

        BigDecimal total = CalculReglement.affecte(List.of(ligne1, ligne2));

        assertAll(
                () -> assertThat(total).isEqualByComparingTo("1500.00"),
                () -> assertThat(CalculRetenue.taux(BASE, total)).isEqualByComparingTo("0.15"),
                () -> assertThat(CalculRetenue.net(BASE, total)).isEqualByComparingTo("8500.00"));
    }

    @Test
    void baseNulle_tauxNul() {
        assertAll(
                () -> assertThat(CalculRetenue.taux(BigDecimal.ZERO, new BigDecimal("10"))).isEqualByComparingTo("0"),
                () -> assertThat(CalculRetenue.taux(null, null)).isEqualByComparingTo("0"),
                () -> assertThat(CalculRetenue.montantRs(BigDecimal.ZERO, new BigDecimal("0.10")))
                        .isEqualByComparingTo("0"));
    }

    @Test
    void arrondi_halfUpADeuxDecimales() {
        BigDecimal rs = CalculRetenue.montantRs(new BigDecimal("100.05"), new BigDecimal("0.10"));

        assertAll(
                () -> assertThat(rs).isEqualTo(new BigDecimal("10.01")),
                () -> assertThat(CalculRetenue.net(new BigDecimal("100.05"), rs)).isEqualTo(new BigDecimal("90.04")),
                () -> assertThat(CalculRetenue.taux(new BigDecimal("3"), BigDecimal.ONE))
                        .isEqualTo(new BigDecimal("0.333333")));
    }
}
