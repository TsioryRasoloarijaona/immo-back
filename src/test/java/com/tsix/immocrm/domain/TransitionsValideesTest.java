package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import static com.tsix.immocrm.domain.StatutDossier.*;
import static org.assertj.core.api.Assertions.assertThat;

class TransitionsValideesTest {

    @Test
    void reservation_versFormalise_estValide() {
        assertThat(TransitionsValidees.estValide(RESERVATION, FORMALISE)).isTrue();
    }

    @Test
    void reservation_versMinute_estInvalide() {
        assertThat(TransitionsValidees.estValide(RESERVATION, MINUTE)).isFalse();
    }

    @Test
    void envoiAuNotaire_aPlusieursTransitionsAutorisees() {
        assertThat(TransitionsValidees.transitionsAutorisees(ENVOI_AU_NOTAIRE))
                .containsExactlyInAnyOrder(MINUTE, COMPROMIS, DESISTEMENT, CREDIT_EN_COURS, CONCRETISATION, ANNULATION);
    }

    @Test
    void desistement_estTerminal() {
        assertThat(TransitionsValidees.transitionsAutorisees(DESISTEMENT)).isEmpty();
    }

    @Test
    void statutDossier_fromCode_matchesRealDbCode() {
        assertThat(StatutDossier.fromCode("ENVOI AU NOTAIRE")).isEqualTo(ENVOI_AU_NOTAIRE);
    }

    @Test
    void statutDossier_sorties_sontAnnulationEtDesistement() {
        assertThat(ANNULATION.isSortie()).isTrue();
        assertThat(DESISTEMENT.isSortie()).isTrue();
        assertThat(RESERVATION.isSortie()).isFalse();
    }
}
