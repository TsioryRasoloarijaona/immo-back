package com.tsix.immocrm.domain.faisabilite;

import com.tsix.immocrm.domain.faisabilite.TresorerieFaisabilite.Ligne;
import com.tsix.immocrm.domain.faisabilite.TresorerieFaisabilite.Type;
import com.tsix.immocrm.dto.patrimoine.faisabilite.MoisTresorerieDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TresorerieFaisabiliteDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Calculs purs de FEATURE-patrimoine-faisabilite-workflow.md §2.5-2.6 (invariant I6). */
class CalculsFaisabiliteTest {

    private static final BigDecimal M = new BigDecimal("1000000");

    /** n = 3, σ = 1 : g = M φ(−0,5) = 352 065,3267 (OMRAN, double) ; bords (M − g) / 2 ; Σ exacte. */
    @Test
    void montants_n3_sigma1_formuleOmran_sommeExacte() {
        List<BigDecimal> lignes = RepartitionGauss.montants(3, BigDecimal.ONE, M);

        assertAll(
                () -> assertThat(lignes.get(1)).isCloseTo(new BigDecimal("352065.3267"), within(new BigDecimal("0.0001"))),
                () -> assertThat(lignes.get(0)).isCloseTo(new BigDecimal("323967.3366"), within(new BigDecimal("0.0001"))),
                () -> assertThat(lignes.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(M));
    }

    /** Cas où OMRAN lève (voisin d'indice −1) : n = 2, q = 3 → [2, 2] puis un retrait en fin → [2, 1]. */
    @Test
    void quantites_n2_q3_retraitEnFin() {
        assertThat(RepartitionGauss.quantites(2, BigDecimal.ONE, 3)).containsExactly(2, 1);
    }

    @Test
    void quantites_nNul_aucuneLigne() {
        assertThat(RepartitionGauss.quantites(0, BigDecimal.ONE, 10)).isEmpty();
    }

    /** I6 : un point par mois, trous compris ; solde final = Σ crédits − Σ débits. */
    @Test
    void serie_soldeFinal_egalCreditsMoinsDebits_moisContinus() {
        LocalDateTime janvier = LocalDateTime.of(2026, 1, 15, 0, 0);
        List<Ligne> lignes = List.of(new Ligne(Type.FINANCEMENT, janvier, new BigDecimal("1000"), null),
                new Ligne(Type.DEPENSE, janvier, new BigDecimal("300"), null),
                new Ligne(Type.CONCRETISATION, janvier.plusMonths(3), new BigDecimal("50"), null),
                new Ligne(Type.REMBOURSEMENT, janvier.plusMonths(3), new BigDecimal("200"), null));

        TresorerieFaisabiliteDto serie = TresorerieFaisabilite.serie(lignes, Map.of(), true);

        List<MoisTresorerieDto> mois = serie.mois();
        assertAll(
                () -> assertThat(mois).hasSize(4),
                () -> assertThat(mois.get(0).solde()).isEqualByComparingTo("700"),
                () -> assertThat(mois.get(3).solde()).isEqualByComparingTo("550"),
                () -> assertThat(serie.lignes()).hasSize(4));
    }

    @Test
    void serie_sansFinancement_creditsEtRemboursementsExclus() {
        LocalDateTime date = LocalDateTime.of(2026, 1, 1, 0, 0);
        List<Ligne> lignes = List.of(new Ligne(Type.FINANCEMENT, date, new BigDecimal("1000"), null),
                new Ligne(Type.DEPENSE, date, new BigDecimal("300"), null));

        assertThat(TresorerieFaisabilite.serie(lignes, Map.of(), false).mois()).singleElement()
                .satisfies(m -> assertThat(m.solde()).isEqualByComparingTo("-300"));
    }
}
