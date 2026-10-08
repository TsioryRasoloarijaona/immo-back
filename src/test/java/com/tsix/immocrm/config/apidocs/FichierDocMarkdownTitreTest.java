package com.tsix.immocrm.config.apidocs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Lecture des titres sans regex (FIX-sonar-fiabilite.md, corrections 4 et 5). */
class FichierDocMarkdownTitreTest {

    @Test
    void titre_niveauxEtNomSansBlancsAutour() {
        assertThat(FichierDocMarkdown.titre("# ExempleController")).isEqualTo(new FichierDocMarkdown.Titre(1, "ExempleController"));
        assertThat(FichierDocMarkdown.titre("## lire")).isEqualTo(new FichierDocMarkdown.Titre(2, "lire"));
        assertThat(FichierDocMarkdown.titre("### param id   \t")).isEqualTo(new FichierDocMarkdown.Titre(3, "param id"));
        assertThat(FichierDocMarkdown.titre("##\t  reponse 404  ")).isEqualTo(new FichierDocMarkdown.Titre(2, "reponse 404"));
        assertThat(FichierDocMarkdown.titre("## # pas un titre de plus")).isEqualTo(new FichierDocMarkdown.Titre(2, "# pas un titre de plus"));
    }

    @Test
    void titre_pasUnTitre() {
        assertThat(FichierDocMarkdown.titre("#### Détail")).isNull();
        assertThat(FichierDocMarkdown.titre("##sansBlanc")).isNull();
        assertThat(FichierDocMarkdown.titre("##")).isNull();
        assertThat(FichierDocMarkdown.titre("## ")).isNull();
        assertThat(FichierDocMarkdown.titre("##  \t ")).isNull();
        assertThat(FichierDocMarkdown.titre(" ## indenté")).isNull();
        assertThat(FichierDocMarkdown.titre("texte # avec dièse")).isNull();
        assertThat(FichierDocMarkdown.titre("")).isNull();
    }

    @Test
    void lire_titreAvecBlancsDeFinEtQuatreDiesesResteDuTexte() {
        FichierDocMarkdown f = FichierDocMarkdown.lire("x.md", "# Titre  \n\n## lire   \n\n#### Note\ncorps\n");
        assertThat(f.erreurs).isEmpty();
        assertThat(f.titre).isEqualTo("Titre");
        assertThat(f.sections).containsOnlyKeys("lire");
        assertThat(f.sections.get("lire").texte()).isEqualTo("#### Note\ncorps");
    }

    @Test
    void nomSimple_apresLeDernierPoint() {
        assertThat(DocsApiMarkdownConfig.nomSimple("com.tsix.immocrm.dto.DossierDto")).isEqualTo("DossierDto");
        assertThat(DocsApiMarkdownConfig.nomSimple("com.tsix.immocrm.dto.Externe$Interne")).isEqualTo("Interne");
        assertThat(DocsApiMarkdownConfig.nomSimple("SansPaquet")).isEqualTo("SansPaquet");
    }
}
