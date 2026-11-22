package com.tsix.immocrm.domain.achat;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculReglementTest {

    @Test
    void affecteSommeToutesLesLignesEtIgnoreLesMontantsAbsents() {
        assertThat(CalculReglement.affecte(Arrays.asList(new BigDecimal("300"), null, new BigDecimal("200.50"))))
                .isEqualByComparingTo("500.50");
        assertThat(CalculReglement.affecte(List.of())).isEqualByComparingTo("0");
    }

    @Test
    void resteEgalMontantMoinsAffecte() {
        assertThat(CalculReglement.resteAAffecter(new BigDecimal("800"), new BigDecimal("300")))
                .isEqualByComparingTo("500");
        assertThat(CalculReglement.resteAAffecter(null, null)).isEqualByComparingTo("0");
    }

    @Test
    void depassementJugeApresArrondiALaDevise() {
        assertThat(CalculReglement.depasse(new BigDecimal("100"), new BigDecimal("100.004"))).isFalse();
        assertThat(CalculReglement.depasse(new BigDecimal("100"), new BigDecimal("100.01"))).isTrue();
        assertThat(CalculReglement.depasse(new BigDecimal("100"), new BigDecimal("100"))).isFalse();
    }

    @Test
    void ligneAZeroApresArrondi() {
        assertThat(CalculReglement.estNul(new BigDecimal("0.004"))).isTrue();
        assertThat(CalculReglement.estNul(null)).isTrue();
        assertThat(CalculReglement.estNul(new BigDecimal("0.01"))).isFalse();
    }
}
