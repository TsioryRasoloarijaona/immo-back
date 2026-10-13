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
                .containsExactlyInAnyOrder(MINUTE, COMPROMIS, DESISTEMENT, CONCRETISATION, ANNULATION,
                        LIVRAISON_PROVISOIRE);
    }

    @Test
    void desistement_estTerminal() {
        assertThat(TransitionsValidees.transitionsAutorisees(DESISTEMENT)).isEmpty();
    }

    /** B2 : la table est la copie de StrWorkFlow (yesRemsProd), 34 transitions. */
    @Test
    void table_copieExacteDeStrWorkFlow() {
        int total = java.util.Arrays.stream(StatutDossier.values())
                .mapToInt(s -> TransitionsValidees.transitionsAutorisees(s).size())
                .sum();
        assertThat(total).isEqualTo(34);
    }

    @Test
    void desistementPossibleDepuisMeilleurtauxCompromisEtCredits() {
        assertThat(java.util.List.of(MEILLEURTAUX, COMPROMIS, CREDIT_EN_COURS, CREDIT_DEBLOQUE))
                .isNotEmpty()
                .allMatch(depuis -> TransitionsValidees.estValide(depuis, DESISTEMENT));
    }

    @Test
    void transitionsAjouteesDepuisStrWorkFlow() {
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThat(TransitionsValidees.estValide(CREDIT_EN_COURS, CONCRETISATION)).isTrue(),
                () -> assertThat(TransitionsValidees.estValide(ENVOI_AU_NOTAIRE, LIVRAISON_PROVISOIRE)).isTrue(),
                () -> assertThat(TransitionsValidees.estValide(LIVRAISON_PROVISOIRE, MINUTE)).isTrue());
    }

    @Test
    void transitionsAbsentesDeStrWorkFlow_refusees() {
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThat(TransitionsValidees.estValide(ENVOI_AU_NOTAIRE, CREDIT_EN_COURS)).isFalse(),
                () -> assertThat(TransitionsValidees.estValide(FORMALISE, LIVRAISON_PROVISOIRE)).isFalse());
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
