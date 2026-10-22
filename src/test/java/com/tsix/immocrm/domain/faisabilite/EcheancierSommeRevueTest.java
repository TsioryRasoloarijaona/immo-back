package com.tsix.immocrm.domain.faisabilite;

import com.tsix.immocrm.domain.faisabilite.FinancementFaisabilite.Echeance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seconde revue argent P8b-1 (invariant I2 : Σ base = déblocage) : les bases sont stockées à l'échelle 6 ; sans
 * reliquat sur la dernière échéance, la somme stockée s'écarte du déblocage et {@code ecartBase} ≠ 0 sur un échéancier généré.
 */
class EcheancierSommeRevueTest {

    private static BigDecimal sommeStockee(List<Echeance> lignes) {
        return lignes.stream().map(e -> BilanFaisabilite.r(e.base())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void troisEcheances_sommeDesBasesStockees_egaleLeDeblocage() {
        List<Echeance> lignes = FinancementFaisabilite.echeancier(LocalDateTime.of(2026, 1, 15, 0, 0),
                new BigDecimal("1000000"), 3, 1, 0, BigDecimal.ZERO);

        assertThat(sommeStockee(lignes)).isEqualByComparingTo("1000000");
    }

    @Test
    void deuxCentQuaranteEcheances_ecartAuDelaDeLaToleranceDeLaSpec() {
        List<Echeance> lignes = FinancementFaisabilite.echeancier(LocalDateTime.of(2026, 1, 15, 0, 0),
                new BigDecimal("1000000"), 240, 1, 0, BigDecimal.ZERO);

        assertThat(sommeStockee(lignes).subtract(new BigDecimal("1000000")).abs()).isLessThanOrEqualTo(new BigDecimal("0.00001"));
    }
}
