package com.tsix.immocrm.domain.note;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class HtmlNoteTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "texte simple|texte simple",
            "<p>ok</p>|<p>ok</p>",
            "<P CLASS='x'>maj</P>|<p>maj</p>",
            "a<br/>b|a<br>b",
            "<img src=x onerror=alert(1)>image|image",
            "<a href='javascript:x()'>lien</a>|lien",
            "<script>alert(1)</script>reste|reste",
            "<STYLE>p{}</STYLE>reste|reste",
            "<!-- commentaire -->reste|reste",
            "1 < 2 > 0|1 &lt; 2 &gt; 0",
            "<p>\"guillemets\"</p>|<p>&quot;guillemets&quot;</p>"
    })
    void assainir_listeBlancheSansAttribut(String entree, String attendu) {
        assertThat(HtmlNote.assainir(entree)).isEqualTo(attendu);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"<p></p>|true", "<p> &nbsp; </p>|true", "<p>x</p>|false"})
    void estVide(String html, boolean vide) {
        assertThat(HtmlNote.estVide(HtmlNote.assainir(html))).isEqualTo(vide);
    }

    @Test
    void baliseJamaisFermee_tresLongue_traiteeEnTempsLineaire() {
        String entree = "<a" + "b".repeat(200_000);

        String sortie = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> HtmlNote.assainir(entree));

        assertThat(sortie).startsWith("&lt;ab").hasSize(entree.length() + 3);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "<p>ok</p>", "1 < 2 > 0", "<img src=x onerror=alert(1)>image", "<p>\"guillemets\"</p>"})
    void assainir_idempotent_relectureSansDoubleEchappement(String entree) {
        String une = HtmlNote.assainir(entree);

        assertThat(HtmlNote.assainir(une)).isEqualTo(une);
    }
}
