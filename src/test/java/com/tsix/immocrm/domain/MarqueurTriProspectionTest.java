package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MarqueurTriProspection} — la normalisation et, surtout, ce qu'elle refuse de reconnaître.
 *
 * <p>Le risque de cette classe n'est pas de rater un marqueur : c'est d'en inventer un. La colonne
 * {@code TempField1} porte 1 239 lignes de texte libre qui ne sont pas des marqueurs — typologies
 * recherchées, villes, budgets. Une correspondance trop généreuse (par préfixe, par
 * {@code contains}) les étiquetterait à tort, et le front afficherait « Doublon » sur une fiche
 * qui dit « doublon tribeca/bel air », c'est-à-dire une note de travail. D'où le poids donné ici
 * aux cas <em>négatifs</em>.
 */
class MarqueurTriProspectionTest {

    @ParameterizedTest
    @ValueSource(strings = {"Doublon", "doublon", "DOUBLON", "  doublon  ", "doublan"})
    void reconnaitLesVariantesDeDoublon(String valeur) {
        assertThat(MarqueurTriProspection.depuis(valeur))
                .isEqualTo(MarqueurTriProspection.DOUBLON);
    }

    /** « Traité » et « traite » coexistent en base : la comparaison ignore les accents. */
    @ParameterizedTest
    @ValueSource(strings = {"Traité", "traité", "traite", "TRAITÉ"})
    void reconnaitTraiteAvecEtSansAccent(String valeur) {
        assertThat(MarqueurTriProspection.depuis(valeur))
                .isEqualTo(MarqueurTriProspection.TRAITE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"spam", "Spam", "SPAM"})
    void reconnaitSpam(String valeur) {
        assertThat(MarqueurTriProspection.depuis(valeur))
                .isEqualTo(MarqueurTriProspection.SPAM);
    }

    /**
     * Les valeurs réellement majoritaires de la colonne côté prospection. Aucune ne doit être
     * reconnue — c'est la typologie recherchée par le prospect, pas un marqueur de tri.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "salon_+_2_chambres", "salon_+_1_chambre", "salon_+_3_chambres",
            "Casablanca", "Rabat", "1_100_000_dhs", "2_000_000_dhs",
            "1_chambre_+_salon_à_partir_de_800.000_dhs"
    })
    void neReconnaitPasLeTexteLibreDeQualification(String valeur) {
        assertThat(MarqueurTriProspection.depuis(valeur)).isNull();
    }

    /**
     * Le cas qui justifie la correspondance exacte. Cette valeur existe en base ; la traiter comme
     * un doublon par préfixe reviendrait à décider à la place de l'utilisateur.
     */
    @Test
    void neReconnaitPasUnMarqueurNoyeDansUnePhrase() {
        assertThat(MarqueurTriProspection.depuis("doublon tribeca/bel air")).isNull();
        assertThat(MarqueurTriProspection.depuis("veut faire une visite")).isNull();
    }

    @Test
    void videEtNullNeSontPasDesMarqueurs() {
        assertThat(MarqueurTriProspection.depuis(null)).isNull();
        assertThat(MarqueurTriProspection.depuis("")).isNull();
        assertThat(MarqueurTriProspection.depuis("   ")).isNull();
    }

    /**
     * « null » écrit en toutes lettres existe en base (2 lignes côté pré-prospection, 13 côté
     * prospection) : c'est une chaîne, pas une absence, et surtout pas un marqueur.
     */
    @Test
    void laChaineNullNEstPasUnMarqueur() {
        assertThat(MarqueurTriProspection.depuis("null")).isNull();
    }
}
