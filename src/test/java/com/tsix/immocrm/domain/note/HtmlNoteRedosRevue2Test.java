package com.tsix.immocrm.domain.note;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Revue 2 FEATURE-patrimoine-audit-actions.md §10 point 3 : aucune regex de {@link HtmlNote} ne doit être super-linéaire
 * sur une entrée utilisateur. {@code BLOCS_DANGEREUX} rescanne jusqu'à la fin du texte depuis chaque ouverture non
 * fermée ({@code <!--}, {@code <script}) : O(n²).
 */
class HtmlNoteRedosRevue2Test {

    @ParameterizedTest
    @ValueSource(strings = {"<!--", "<script>", "<script "})
    void ouverturesJamaisFermees_repetees_traiteesEnTempsLineaire(String motif) {
        String entree = motif.repeat(200_000 / motif.length());

        String sortie = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> HtmlNote.assainir(entree));

        assertThat(sortie).doesNotContain("<script").doesNotContain("<!--");
    }
}
