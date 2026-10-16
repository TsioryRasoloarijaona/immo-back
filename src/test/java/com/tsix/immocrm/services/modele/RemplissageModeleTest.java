package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.dto.dossier.ActeReservationData.LignePaiement;
import com.tsix.immocrm.dto.dossier.ActeReservationData.SousArticle;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Génération : échappement, conditions, lignes répétées, numérotation (FEATURE-modeles-documents.md §5, §11). */
class RemplissageModeleTest {

    private static final Catalogue<ActeReservationData> CATALOGUE = CatalogueDossierVente.CATALOGUE;

    static ActeReservationData donnees(String nomClient, boolean terrain, String telephone, int echeances) {
        List<LignePaiement> lignes = IntStream.range(0, echeances)
                .mapToObj(i -> new LignePaiement(new BigDecimal("1000.00").add(BigDecimal.valueOf(i)),
                        new BigDecimal("10"), LocalDate.of(2027, 1 + i, 15)))
                .toList();
        return new ActeReservationData("APF 2022/0012", "Monsieur", nomClient, "A742122", "Casablanca", telephone,
                "AYAD PRIVATE", null, "MAYLINE", terrain, "APPARTEMENT", "APF-0-12", "IMM01", "1er étage",
                new BigDecimal("60.00"), List.of(new SousArticle("TERRASSE", null)), new BigDecimal("1200000"),
                new BigDecimal("120000"), new BigDecimal("10.00"), lignes, new BigDecimal("15"), null, null,
                new BigDecimal("2.5"), 12, null, 24, "Casablanca", "Karim", LocalDate.of(2026, 10, 16));
    }

    private static Element remplir(String html, ActeReservationData data) {
        Element corps = ContenuModele.analyser(html, CATALOGUE).corps().clone();
        return RemplissageModele.remplir(corps, CATALOGUE, CATALOGUE.valeurs(data));
    }

    @Test
    void valeurAvecBalise_resteDuTexteEchappe() {
        Element corps = remplir("<p>Client : <span data-champ=\"client.nom\">Nom</span></p>",
                donnees("<script>alert(1)</script> & co", false, null, 0));

        assertThat(corps.select("script")).isEmpty();
        assertThat(corps.html()).contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; co");
        assertThat(corps.text()).isEqualTo("Client : <script>alert(1)</script> & co");
    }

    @Test
    void champsFormatesCommeLesDocumentsDuDossier() {
        Element corps = remplir("<p><span data-champ=\"dossier.prixVente\">P</span>|"
                        + "<span data-champ=\"dossier.avancePct\">A</span>|<span data-champ=\"dossier.dateEdition\">D</span>|"
                        + "<span data-champ=\"article.estTerrain\">T</span>|<span data-champ=\"penalite.transfertProduit\">X</span></p>",
                donnees("Nom", false, null, 0));

        assertThat(corps.text()).isEqualTo("1 200 000.00 DH|10.00 %|16/10/2026|Non|0.00 DH");
    }

    @Test
    void conditionBooleenne_vraieAfficheeFausseRetiree() {
        String html = "<p data-si=\"article.estTerrain\">terrain</p><p data-si=\"article.estTerrain\" data-valeur=\"false\">bâti</p>";

        assertThat(remplir(html, donnees("N", true, null, 0)).text()).isEqualTo("terrain");
        assertThat(remplir(html, donnees("N", false, null, 0)).text()).isEqualTo("bâti");
    }

    @Test
    void conditionSurTexte_afficheeSeulementSiRenseigne() {
        String html = "<p>A<span data-si=\"client.telephone\"> Tél : <span data-champ=\"client.telephone\">T</span></span></p>";

        assertThat(remplir(html, donnees("N", false, "0612", 0)).text()).isEqualTo("A Tél : 0612");
        assertThat(remplir(html, donnees("N", false, null, 0)).text()).isEqualTo("A");
        assertThat(remplir(html, donnees("N", false, "  ", 0)).text()).isEqualTo("A");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void ligneRepetee_uneParElementEtRetireeSiVide(int echeances) {
        String html = "<table><tbody><tr><th>Montant</th></tr><tr data-repeter=\"echeancesSolde\">"
                + "<td><span data-champ=\"echeance.montant\">M</span></td><td><span data-champ=\"client.nom\">N</span></td>"
                + "</tr></tbody></table>";

        Element corps = remplir(html, donnees("Nom", false, null, echeances));

        assertThat(corps.select("tr")).hasSize(1 + echeances);
        assertThat(corps.select("[data-repeter], [data-champ]")).isEmpty();
        if (echeances == 3) {
            assertThat(corps.select("td:eq(0)").eachText())
                    .containsExactly("1 000.00 DH", "1 001.00 DH", "1 002.00 DH");
        }
    }

    @Test
    void conditionDansUneLigneRepetee_testeLaValeurDeLElement() {
        String html = "<p data-repeter=\"sousArticles\">et un <span data-champ=\"sousArticle.type\">T</span>"
                + "<span data-si=\"sousArticle.superficie\"> de <span data-champ=\"sousArticle.superficie\">S</span> m²</span></p>";

        assertThat(remplir(html, donnees("N", false, null, 0)).text()).isEqualTo("et un TERRASSE");
    }

    @Test
    void numerotation_sauteLesBlocsMasques() {
        String html = "<h2 data-numero></h2><h2 data-numero>DEUX</h2>"
                + "<div data-si=\"article.estTerrain\" data-valeur=\"false\"><h2 data-numero>MASQUÉ</h2></div>"
                + "<h2 data-numero>QUATRE</h2>";

        Element corps = remplir(html, donnees("N", true, null, 0));

        assertThat(corps.select("h2").eachText()).containsExactly("1-", "2- DEUX", "3- QUATRE");
        assertThat(corps.select("[data-numero]")).isEmpty();
    }

    @Test
    void sautDePage_devientUnSautCss() {
        Element corps = remplir("<p>a</p><div data-saut-page></div><p>b</p>", donnees("N", false, null, 0));

        assertThat(corps.select("div").attr("style")).isEqualTo("page-break-before: always");
    }
}
