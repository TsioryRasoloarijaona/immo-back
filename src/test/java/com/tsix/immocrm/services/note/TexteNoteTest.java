package com.tsix.immocrm.services.note;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** FEATURE-patrimoine-audit-actions.md §11.8 : passage texte ↔ HTML d'une note, linéaire sur une saisie hostile. */
class TexteNoteTest {

    @Test
    void entitesNumeriquesEtInconnues() {
        assertThat(TexteNote.versTexte("&#233;t&#xE9; &copy; &amp")).isEqualTo("été &copy; &amp");
    }

    @Test
    void chevronSansFermeture_resteDuTexte() {
        assertThat(TexteNote.versTexte("<b>a</b> < b")).isEqualTo("a < b");
    }

    @Test
    void retourChariotIgnore_sautDeLigneEnBr() {
        assertThat(TexteNote.versHtml("a\r\nb")).isEqualTo("a<br>b");
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void saisieHostile_parcoursLineaire() {
        String chevrons = "<".repeat(200_000);
        String esperluettes = "&".repeat(200_000);

        assertThat(TexteNote.versTexte(chevrons + esperluettes)).hasSize(400_000);
    }
}
