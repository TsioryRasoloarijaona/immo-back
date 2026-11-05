package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.config.SynchronisationDescendante.Regime.BASELINE;
import static com.tsix.immocrm.config.SynchronisationDescendante.Regime.DELTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private static final LigneEcrite COMMANDE = new LigneEcrite("BuyDocument", UUID.randomUUID());
    private static final LigneEcrite FACTURE = new LigneEcrite("BuyDocument", UUID.randomUUID());
    private static final LigneEcrite SUPPRIMEE = new LigneEcrite("BuyDocument", UUID.randomUUID());

    /** Copie simulée : la commande cite la facture, refusée (clé étrangère) tant que la facture n'y est pas. */
    private final Set<LigneEcrite> copie = new HashSet<>();

    private boolean fusionner(LigneEcrite ligne) {
        if (ligne.equals(SUPPRIMEE)) {
            return false;
        }
        if (ligne.equals(COMMANDE) && !copie.contains(FACTURE)) {
            throw new DataIntegrityViolationException("BuyDocument_BuyDocument_FK");
        }
        copie.add(ligne);
        return true;
    }

    @Test
    void uneLigneQuiCiteUneLigneDuMemeDeltaPlusLoinPasseAuTourSuivant() {
        List<LigneEcrite> suppressions = new ArrayList<>();

        int fusionnees = SynchronisationDescendante.fusionnerChangements(
                List.of(COMMANDE, SUPPRIMEE, FACTURE), this::fusionner, suppressions);

        assertThat(fusionnees).isEqualTo(2);
        assertThat(copie).containsExactlyInAnyOrder(COMMANDE, FACTURE);
        assertThat(suppressions).containsExactly(SUPPRIMEE);
    }

    @Test
    void uneLigneDontLeParentNArriveJamaisFaitToujoursEchouerLaTable() {
        List<LigneEcrite> suppressions = new ArrayList<>();
        List<LigneEcrite> changements = List.of(COMMANDE);

        assertThatThrownBy(() -> SynchronisationDescendante.fusionnerChangements(
                changements, this::fusionner, suppressions))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(copie).isEmpty();
    }
}
