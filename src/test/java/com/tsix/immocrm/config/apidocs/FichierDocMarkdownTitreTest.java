package com.tsix.immocrm.config.apidocs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Lecture des titres sans regex (FIX-sonar-fiabilite.md, corrections 4 et 5). */
class FichierDocMarkdownTitreTest {

    @Test
    void titre_niveauxEtNomSansBlancsAutour() {
        assertThat(FichierDocMarkdown.lireTitre("# ExempleController")).isEqualTo(new FichierDocMarkdown.Titre(1, "ExempleController"));
        assertThat(FichierDocMarkdown.lireTitre("## lire")).isEqualTo(new FichierDocMarkdown.Titre(2, "lire"));
        assertThat(FichierDocMarkdown.lireTitre("### param id   \t")).isEqualTo(new FichierDocMarkdown.Titre(3, "param id"));
        assertThat(FichierDocMarkdown.lireTitre("##\t  reponse 404  ")).isEqualTo(new FichierDocMarkdown.Titre(2, "reponse 404"));
        assertThat(FichierDocMarkdown.lireTitre("## # pas un titre de plus")).isEqualTo(new FichierDocMarkdown.Titre(2, "# pas un titre de plus"));
    }

    @Test
    void titre_pasUnTitre() {
        assertThat(FichierDocMarkdown.lireTitre("#### Détail")).isNull();
        assertThat(FichierDocMarkdown.lireTitre("##sansBlanc")).isNull();
        assertThat(FichierDocMarkdown.lireTitre("##")).isNull();
        assertThat(FichierDocMarkdown.lireTitre("## ")).isNull();
        assertThat(FichierDocMarkdown.lireTitre("##  \t ")).isNull();
        assertThat(FichierDocMarkdown.lireTitre(" ## indenté")).isNull();
        assertThat(FichierDocMarkdown.lireTitre("texte # avec dièse")).isNull();
        assertThat(FichierDocMarkdown.lireTitre("")).isNull();
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
