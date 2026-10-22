package com.tsix.immocrm.domain.faisabilite;

import com.tsix.immocrm.domain.faisabilite.FinancementFaisabilite.Echeance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EcheancierEntreesNullesTest {

    private static final LocalDateTime DEBLOCAGE = LocalDateTime.of(2026, 1, 31, 0, 0);

    @Test
    void echeancier_montantNul_donneDesEcheancesAZero() {
        List<Echeance> lignes = FinancementFaisabilite.echeancier(DEBLOCAGE, null, 3, null, null, null);

        assertThat(lignes).hasSize(3)
                .extracting(Echeance::aRegler)
                .allSatisfy(v -> assertThat(v).isZero());
    }

    @Test
    void echeancier_periodiciteEtGraceNulles_pasMensuelSansGrace() {
        List<Echeance> lignes = FinancementFaisabilite.echeancier(DEBLOCAGE, new BigDecimal("300"), 3, null, null,
                BigDecimal.ZERO);

        assertThat(lignes).extracting(Echeance::date).containsExactly(DEBLOCAGE,
                LocalDateTime.of(2026, 2, 28, 0, 0), LocalDateTime.of(2026, 3, 31, 0, 0));
    }
}
