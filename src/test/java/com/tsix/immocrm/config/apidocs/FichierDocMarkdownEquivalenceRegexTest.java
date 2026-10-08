package com.tsix.immocrm.config.apidocs;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Revue FIX-sonar-fiabilite §5 : le lecteur manuel de titres rend ce que rendait l'ancienne regex. */
class FichierDocMarkdownEquivalenceRegexTest {

    private static final Pattern ANCIEN = Pattern.compile("^(#{1,3})\\s+(.+?)\\s*$");

    private static FichierDocMarkdown.Titre ancien(String ligne) {
        Matcher m = ANCIEN.matcher(ligne);
        return m.matches() ? new FichierDocMarkdown.Titre(m.group(1).length(), m.group(2)) : null;
    }

    @Test
    void toutesLesLignesDesFichiersReels_memeResultat() throws Exception {
        Resource[] fichiers = new PathMatchingResourcePatternResolver().getResources("classpath*:api-docs/**/*.md");
        assertThat(fichiers).isNotEmpty();
        int titres = 0;
        List<String> ecarts = new ArrayList<>();
        for (Resource r : fichiers) {
            String contenu = r.getContentAsString(StandardCharsets.UTF_8);
            FichierDocMarkdown f = FichierDocMarkdown.lire(r.getFilename(), contenu);
            assertThat(f.erreurs).as(r.getFilename()).isEmpty();
            for (String ligne : contenu.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
                FichierDocMarkdown.Titre a = ancien(ligne);
                if (a != null) titres++;
                if (!java.util.Objects.equals(a, FichierDocMarkdown.titre(ligne))) ecarts.add(r.getFilename() + ": " + ligne);
            }
        }
        assertThat(ecarts).isEmpty();
        assertThat(titres).isGreaterThan(100);
    }

    @Test
    void casLimites_memeResultat() {
        String[] lignes = {
                "# A", "## a b c", "### param  x ", "#\tA", "##\t\tA\t", "#### A", "##### A", "#A", "#", "##", "###",
                "## a#b", "## #", "## ##", "### ### x", " ## A", "\t## A", "## A ", "## A", "##  A",
                "## A ", "## A\u0085", "## A B", "## A ", "## \u0085A", "## A\u000BB", "## A\fB",
                "## A\u000B", "##\u000BA", "## é ü", "## A​", "##\fA", "", "texte",
        };
        for (String l : lignes) {
            assertThat(FichierDocMarkdown.titre(l)).as("« %s »", l).isEqualTo(ancien(l));
        }
    }
}
