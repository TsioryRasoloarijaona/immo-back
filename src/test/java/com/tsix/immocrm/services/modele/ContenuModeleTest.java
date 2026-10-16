package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Liste blanche du HTML restreint (FEATURE-modeles-documents.md §5, §11). */
class ContenuModeleTest {

    private static final Catalogue<?> CATALOGUE = CatalogueDossierVente.CATALOGUE;

    private static void refuse(String html, String message) {
        assertThatThrownBy(() -> ContenuModele.analyser(html, CATALOGUE))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(message);
    }

    private static String png(int octets) {
        byte[] image = new byte[octets];
        image[0] = (byte) 0x89;
        image[1] = 'P';
        image[2] = 'N';
        image[3] = 'G';
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
    }

    @Test
    void acteDeReservationLivre_estConformeALaListeBlanche() {
        ContenuModele.Analyse analyse = ContenuModele.analyser(InitialiseurModeles.contenuActe(), CATALOGUE);

        assertThat(analyse.corps().select("[data-champ]")).isNotEmpty();
    }

    @Test
    void contenuTypeEditeur_estAccepte() {
        String html = "<h2 data-numero style=\"text-align: center\">Titre</h2><p>Bonjour <strong><em><u>x</u></em></strong>"
                + "<br></p><ul><li><p>a</p></li></ul><ol start=\"2\"><li>b</li></ol><hr>"
                + "<table><tbody><tr><th colspan=\"2\" style=\"width: 50%\">T</th></tr>"
                + "<tr data-repeter=\"echeancesSolde\"><td><span data-champ=\"echeance.montant\">Montant</span></td>"
                + "<td style=\"text-align: right; width: 25.5%\">x</td></tr></tbody></table>"
                + "<div data-saut-page></div><div data-si=\"article.estTerrain\" data-valeur=\"false\"><p>Bâti</p></div>"
                + "<img src=\"" + png(10) + "\" alt=\"logo\">";

        ContenuModele.Analyse analyse = ContenuModele.analyser(html, CATALOGUE);

        assertThat(analyse.octetsImages()).isEqualTo(10);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "<script>alert(1)</script>|Élément interdit : <script>",
            "<p>a<iframe src=\"x\"></iframe></p>|Élément interdit : <iframe>",
            "<style>p{color:red}</style>|Élément interdit : <style>",
            "<svg><circle/></svg>|Élément interdit : <svg>",
            "<a href=\"https://x\">lien</a>|Élément interdit : <a>",
            "<p onclick=\"alert(1)\">a</p>|Attribut interdit : onclick sur <p>",
            "<img alt=\"x\" onerror=\"alert(1)\">|Attribut interdit : onerror sur <img>",
            "<p class=\"x\">a</p>|Attribut interdit : class sur <p>",
            "<p style=\"color: red\">a</p>|Style interdit sur <p> : color: red",
            "<p style=\"text-align: center; background: url(http://x)\">a</p>|Style interdit sur <p>",
            "<p style=\"width: 50%\">a</p>|Style interdit sur <p> : width: 50%"
    })
    void elementAttributOuStyleHorsListe_400(String html, String message) {
        refuse(html, message);
    }

    @Test
    void baliseBodyDansLeFragment_neLaissePasPasserSesAttributs() {
        ContenuModele.Analyse analyse = ContenuModele.analyser("<body onload=\"alert(1)\"><p>a</p></body>", CATALOGUE);

        assertThat(analyse.corps().outerHtml()).contains("<p>a</p>").doesNotContain("onload");
    }

    @Test
    void largeurDeCelluleHorsPourcentage_400() {
        refuse("<table><tbody><tr><td style=\"width: 50px\">a</td></tr></tbody></table>", "Style interdit sur <td>");
    }

    @Test
    void imageExterne_400() {
        refuse("<img src=\"https://exemple.com/logo.png\">", "Image refusée");
    }

    @Test
    void imageSvgEnData_400() {
        refuse("<img src=\"data:image/svg+xml;base64,PHN2Zz4=\">", "Image refusée");
    }

    @Test
    void imageDePlusDe500Ko_400() {
        refuse("<img src=\"" + png(500 * 1024 + 1) + "\">", "Image trop lourde");
    }

    @Test
    void imageDontLeContenuNEstPasDuPng_400() {
        refuse("<img src=\"data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4}) + "\">",
                "ne correspond pas à son type");
    }

    @Test
    void imagesDePlusDe2MoParVersion_400() {
        String image = "<img src=\"" + png(450 * 1024) + "\">";
        ContenuModele.Analyse analyse = ContenuModele.analyser(image.repeat(5), CATALOGUE);

        assertThatThrownBy(() -> ContenuModele.verifierPoidsVersion(analyse.octetsImages()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("2 Mo maximum");
    }

    @Test
    void champInconnu_400() {
        refuse("<p><span data-champ=\"client.motDePasse\">x</span></p>", "Champ inconnu : client.motDePasse");
    }

    @Test
    void champDeListeHorsLigneRepetee_400() {
        refuse("<p><span data-champ=\"echeance.montant\">x</span></p>",
                "Champ de liste hors ligne répétée : echeance.montant");
    }

    @Test
    void champDUneAutreListeDansUneLigneRepetee_400() {
        refuse("<p data-repeter=\"sousArticles\"><span data-champ=\"echeance.montant\">x</span></p>",
                "Champ de liste hors ligne répétée : echeance.montant");
    }

    @Test
    void listeInconnue_400() {
        refuse("<p data-repeter=\"reglements\">x</p>", "Liste inconnue : reglements");
    }

    @Test
    void ligneRepeteeImbriquee_400() {
        refuse("<p data-repeter=\"sousArticles\"><span data-repeter=\"echeancesSolde\">x</span></p>",
                "Ligne répétée imbriquée");
    }

    @Test
    void conditionSurChampInconnu_400() {
        refuse("<p data-si=\"client.inconnu\">x</p>", "Champ inconnu : client.inconnu");
    }

    @Test
    void dataValeurSurChampNonBooleen_400() {
        refuse("<p data-si=\"client.telephone\" data-valeur=\"false\">x</p>", "champ booléen");
    }

    @Test
    void dataValeurAutreQueVraiOuFaux_400() {
        refuse("<p data-si=\"article.estTerrain\" data-valeur=\"oui\">x</p>", "true ou false");
    }

    @Test
    void conditionSurChampDeListeDansSaLigne_estAcceptee() {
        String html = "<p data-repeter=\"echeancesSolde\"><span data-si=\"echeance.date\">le "
                + "<span data-champ=\"echeance.date\">Date</span></span></p>";

        assertThat(ContenuModele.analyser(html, CATALOGUE).corps().children()).hasSize(1);
    }
}
