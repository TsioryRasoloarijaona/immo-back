package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.tsix.immocrm.domain.StatutEcheance.A_VENIR;
import static com.tsix.immocrm.domain.StatutEcheance.EN_RETARD;
import static com.tsix.immocrm.domain.StatutEcheance.PARTIEL;
import static com.tsix.immocrm.domain.StatutEcheance.PAYE;
import static com.tsix.immocrm.domain.StatutEcheance.calculer;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verrouille la règle extraite de {@code EcheancierService.buildDto}, où elle vivait sous forme
 * de cascade {@code if/else} produisant quatre littéraux — donc intestable sans monter un
 * {@code ComDocument} et un contexte Spring.
 */
class StatutEcheanceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 9, 2, 12, 0);
    private static final LocalDateTime HIER = MAINTENANT.minusDays(1);
    private static final LocalDateTime DEMAIN = MAINTENANT.plusDays(1);
    private static final BigDecimal MILLE = new BigDecimal("1000.00");

    @Test
    void soldee_estPayee() {
        assertThat(calculer(MILLE, MILLE, HIER, MAINTENANT)).isEqualTo(PAYE);
    }

    /** Un sur-paiement reste PAYE — c'est le comportement qui existait avant l'extraction. */
    @Test
    void surPaiement_resteePayee() {
        assertThat(calculer(MILLE, new BigDecimal("1500.00"), HIER, MAINTENANT)).isEqualTo(PAYE);
    }

    /** B6 (FEATURE-vente-socle.md) : comme l'impayé d'OMRAN, une échéance entamée mais échue est en retard. */
    @Test
    void partiellementRegleeEtEchue_estEnRetard() {
        assertThat(calculer(MILLE, new BigDecimal("400.00"), HIER, MAINTENANT)).isEqualTo(EN_RETARD);
    }

    @Test
    void partiellementRegleeEtPasEncoreDue_estPartielle() {
        assertThat(calculer(MILLE, new BigDecimal("400.00"), DEMAIN, MAINTENANT)).isEqualTo(PARTIEL);
    }

    /** « Échéance ≤ aujourd'hui » : due dans la journée, même plus tard que l'heure courante. */
    @Test
    void echeanceDuJour_estEnRetard() {
        assertThat(calculer(MILLE, BigDecimal.ZERO, MAINTENANT.plusHours(3), MAINTENANT)).isEqualTo(EN_RETARD);
    }

    @Test
    void rienDeRegleEtDateDepassee_estEnRetard() {
        assertThat(calculer(MILLE, BigDecimal.ZERO, HIER, MAINTENANT)).isEqualTo(EN_RETARD);
    }

    @Test
    void rienDeRegleEtDateFuture_estAVenir() {
        assertThat(calculer(MILLE, BigDecimal.ZERO, DEMAIN, MAINTENANT)).isEqualTo(A_VENIR);
    }

    /** Sans date d'échéance, rien ne permet de conclure au retard. */
    @Test
    void sansDateEcheance_jamaisEnRetard() {
        assertThat(calculer(MILLE, BigDecimal.ZERO, null, MAINTENANT)).isEqualTo(A_VENIR);
    }

    /** {@code ComDocument.Amount} et la somme des règlements peuvent être nuls en base. */
    @Test
    void montantsNuls_traitesCommeZero() {
        assertThat(calculer(null, null, DEMAIN, MAINTENANT)).isEqualTo(PAYE);
        assertThat(calculer(MILLE, null, DEMAIN, MAINTENANT)).isEqualTo(A_VENIR);
    }

    /**
     * Les constantes sont la valeur émise telle quelle sur {@code EcheanceDto.statutCalcule} :
     * les renommer casserait le front sans qu'aucun autre test ne le voie.
     */
    @Test
    void nomsDesConstantes_sontLeContratDuFront() {
        assertThat(PAYE.name()).isEqualTo("PAYE");
        assertThat(PARTIEL.name()).isEqualTo("PARTIEL");
        assertThat(EN_RETARD.name()).isEqualTo("EN_RETARD");
        assertThat(A_VENIR.name()).isEqualTo("A_VENIR");
    }
}
