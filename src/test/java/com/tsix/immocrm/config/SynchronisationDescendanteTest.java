package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static com.tsix.immocrm.config.SynchronisationDescendante.Regime.BASELINE;
import static com.tsix.immocrm.config.SynchronisationDescendante.Regime.DELTA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * La règle de choix du régime — la partie du cycle qui se trompe cher : prendre DELTA avec un
 * filigrane périmé, c'est perdre en silence tout ce qui a changé au-delà de la rétention.
 * Le cycle réel demande deux bases et le Change Tracking actif ; il se vérifie à la main
 * (cf. FEATURE-source-de-verite-115.md).
 */
class SynchronisationDescendanteTest {

    @Test
    void sansFiligraneCEstUneBaseline() {
        assertThat(SynchronisationDescendante.regime(null, 100L)).isEqualTo(BASELINE);
    }

    @Test
    void unFiligranePlusVieuxQueLaRetentionForceLaBaseline() {
        // CHANGE_TRACKING_MIN_VALID_VERSION = 50 : le nettoyage a purge tout ce qui precede.
        // Un filigrane a 40 ne peut plus enumerer ses deltas — DELTA perdrait des lignes.
        assertThat(SynchronisationDescendante.regime(40L, 50L)).isEqualTo(BASELINE);
    }

    @Test
    void unMinValideNullSignaleUneTableNonSuivieEtForceLaBaseline() {
        // CHANGE_TRACKING_MIN_VALID_VERSION renvoie NULL si la table n'est pas (plus) suivie.
        assertThat(SynchronisationDescendante.regime(40L, null)).isEqualTo(BASELINE);
    }

    @Test
    void leCasCourantEstUnDelta() {
        assertThat(SynchronisationDescendante.regime(50L, 50L)).isEqualTo(DELTA);
        assertThat(SynchronisationDescendante.regime(120L, 50L)).isEqualTo(DELTA);
    }
}
