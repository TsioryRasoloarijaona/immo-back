package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.CATEGORIE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.CONCRET_LIBRES;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 22 {@code ComTdbConcretisationAndFree} « CONCRÉTISATIONS PAR RAPPORT AUX LIBRES » (FEATURE-tdb-vente-rapports-rythme.md
 * §4.5, §7 « N° 22 », décision du manager 25/10 : sous-article = {@code IsUnderItem}). Projet P actif ; avances en 2070 :
 * <pre>
 *   T1, catégorie A : d1 CONC · d2 ANN (non net) · 1 libre · dossier CONC sur un sous-article (parent racine A libre)
 *                      ⇒ racines 4, concrétisations nettes 2 (sous-article compris)
 *       catégorie B : d3 CONC · 1 libre                       ⇒ racines 2, concrétisations 1
 *   T2, catégorie A : e1 CONC · 3 libres                      ⇒ racines 4, concrétisations 1
 *   T3 : aucun article
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConcretisationsLibresRapportTest {

    private static final String TRANCHES = "tranches";
    private static final String TRANCHE = "tranche";
    private static final String NB_ARTICLES = "nbArticles";
    private static final String NB_CONCRETISATIONS = "nbConcretisations";
    private static final String NB_LIBRES = "nbLibres";
    private static final String TAUX_CONCRETISATION = "tauxConcretisation";
    private static final String TAUX_LIBRES = "tauxLibres";
    private static final String PRIX = "1000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielStk referentielStk;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private Jeu jeu;
    private CfgProject projet;
    private CfgTranche t1;
    private CfgTranche t2;
    private CfgTranche t3;
    private StkItemCategory catA;
    private StkItemCategory catB;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsRythmeSupport.installerR2b(rapports);
        jeu = fixture.jeu("CL22");
        libre = connecte(rapports.compte("cl22." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        t2 = jeu.tranche("T2", projet);
        t3 = jeu.tranche("T3", projet);
        catA = jeu.categorieArticle();
        catB = jeu.categorieArticle();

        avancer(jeu.dossier(t1, st.concretisation(), catA), LocalDate.of(2070, 3, 1));
        avancer(jeu.dossier(t1, st.annulation(), catA), LocalDate.of(2070, 4, 1));
        jeu.articleLibre(t1, PRIX, catA);
        avancer(jeu.dossierSousArticle(t1, st.concretisation(), catA), LocalDate.of(2070, 4, 15));
        avancer(jeu.dossier(t1, st.concretisation(), catB), LocalDate.of(2070, 5, 1));
        jeu.articleLibre(t1, PRIX, catB);

        avancer(jeu.dossier(t2, st.concretisation(), catA), LocalDate.of(2070, 6, 1));
        for (int i = 0; i < 3; i++) {
            jeu.articleLibre(t2, PRIX, catA);
        }
        fixture.relire();
    }

    private void avancer(ComFolder dossier, LocalDate avance) {
        jeu.dater(dossier, avance, null, null, null);
    }

    private String corps(String... categories) throws Exception {
        String[] parametres = new String[categories.length * 2];
        for (int i = 0; i < categories.length; i++) {
            parametres[2 * i] = CATEGORIE;
            parametres[2 * i + 1] = categories[i];
        }
        return appels.corps(demande(CONCRET_LIBRES, libre, List.of(t1, t2, t3), parametres));
    }

    private Map<String, Object> tranches(String... categories) throws Exception {
        List<Map<String, Object>> sections = JsonPath.read(corps(categories), "$.sections");
        return section(sections, TRANCHES);
    }

    private static void verifier(SoftAssertions soft, Map<String, Object> l, String articles, String concretisations,
                                 String libres, String taux, String tauxLibres) {
        soft.assertThat(nombre(l.get(NB_ARTICLES))).as("%s nbArticles", l.get(TRANCHE)).isEqualByComparingTo(articles);
        soft.assertThat(nombre(l.get(NB_CONCRETISATIONS))).as("%s nbConcretisations", l.get(TRANCHE))
                .isEqualByComparingTo(concretisations);
        soft.assertThat(nombre(l.get(NB_LIBRES))).as("%s nbLibres", l.get(TRANCHE)).isEqualByComparingTo(libres);
        soft.assertThat(nombre(l.get(TAUX_CONCRETISATION))).as("%s taux", l.get(TRANCHE)).isEqualByComparingTo(taux);
        soft.assertThat(nombre(l.get(TAUX_LIBRES))).as("%s tauxLibres", l.get(TRANCHE)).isEqualByComparingTo(tauxLibres);
    }

    // ------------------------------------------------------------------ structure (§4.5)

    @Test
    void r22_sectionTranches_colonnesEtTypes() throws Exception {
        Map<String, Object> s = tranches();

        assertThat(noms(s)).containsExactly(TRANCHE, NB_ARTICLES, NB_CONCRETISATIONS, NB_LIBRES, TAUX_CONCRETISATION,
                TAUX_LIBRES);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonne(s, TRANCHE).get("type")).isEqualTo("TEXTE");
        soft.assertThat(colonne(s, NB_ARTICLES).get("type")).isEqualTo("NOMBRE");
        soft.assertThat(colonne(s, NB_LIBRES).get("type")).isEqualTo("NOMBRE");
        soft.assertThat(colonne(s, TAUX_CONCRETISATION).get("type")).isEqualTo("POURCENTAGE");
        soft.assertThat(colonne(s, TAUX_LIBRES).get("type")).isEqualTo("POURCENTAGE");
        soft.assertAll();
    }

    @Test
    void r22_uneLigneParTrancheAvecArticle_codeDeTranche_trieeParCode_trancheSansArticleAbsente() throws Exception {
        Map<String, Object> s = tranches();

        assertThat(lignes(s)).extracting(l -> l.get(TRANCHE)).containsExactly(t1.getCode(), t2.getCode());
    }

    // ------------------------------------------------------------------ chiffres (§7 « N° 22 »)

    @Test
    void r22_sansFiltre_nbLibresEgalArticlesMoinsConcretisationsNettes_sousArticleAuNumerateur() throws Exception {
        Map<String, Object> s = tranches();

        SoftAssertions soft = new SoftAssertions();
        verifier(soft, ligne(s, TRANCHE, t1.getCode()), "6", "3", "3", "50", "50");
        verifier(soft, ligne(s, TRANCHE, t2.getCode()), "4", "1", "3", "25", "75");
        soft.assertAll();
    }

    @Test
    void r22_totaux_sommeDesNombres_tauxImposesSommeSurSomme() throws Exception {
        Map<String, Object> t = totaux(tranches());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(t.get(NB_ARTICLES))).isEqualByComparingTo("10");
        soft.assertThat(nombre(t.get(NB_CONCRETISATIONS))).isEqualByComparingTo("4");
        soft.assertThat(nombre(t.get(NB_LIBRES))).isEqualByComparingTo("6");
        soft.assertThat(nombre(t.get(TAUX_CONCRETISATION))).as("4 ÷ 10, pas 50 + 25").isEqualByComparingTo("40");
        soft.assertThat(nombre(t.get(TAUX_LIBRES))).as("6 ÷ 10").isEqualByComparingTo("60");
        soft.assertAll();
    }

    @Test
    void r22_filtreCategorie_reduitNumerateurEtDenominateur() throws Exception {
        Map<String, Object> s = tranches(catA.getId().toString());

        SoftAssertions soft = new SoftAssertions();
        verifier(soft, ligne(s, TRANCHE, t1.getCode()), "4", "2", "2", "50", "50");
        verifier(soft, ligne(s, TRANCHE, t2.getCode()), "4", "1", "3", "25", "75");
        soft.assertThat(nombre(totaux(s).get(TAUX_CONCRETISATION))).as("3 ÷ 8").isEqualByComparingTo("37.50");
        soft.assertAll();
    }

    @Test
    void r22_filtreCategorie_trancheSansArticleDeLaCategorie_absente() throws Exception {
        Map<String, Object> s = tranches(catB.getId().toString());

        assertThat(lignes(s)).extracting(l -> l.get(TRANCHE)).containsExactly(t1.getCode());
        SoftAssertions soft = new SoftAssertions();
        verifier(soft, lignes(s).get(0), "2", "1", "1", "50", "50");
        soft.assertAll();
    }

    @Test
    void r22_filtreMultiple_deuxCategories_egalSansFiltre() throws Exception {
        Map<String, Object> deux = tranches(catA.getId().toString(), catB.getId().toString());

        assertThat(lignes(deux)).isEqualTo(lignes(tranches()));
    }

    // ------------------------------------------------------------------ avertissement, graphique, paramètres

    @Test
    void r22_concretisationSurSousArticle_avertissement() throws Exception {
        List<String> avertissements = JsonPath.read(corps(), "$.avertissements");

        assertThat(avertissements).anyMatch(a -> a.toLowerCase().contains("sous-article"));
    }

    @Test
    void r22_sansConcretisationSurSousArticle_pasDAvertissementSousArticle() throws Exception {
        String sansSous = appels.corps(demande(CONCRET_LIBRES, libre, List.of(t2)));
        List<String> avertissements = JsonPath.read(sansSous, "$.avertissements");

        assertThat(avertissements.stream().filter(a -> a.toLowerCase().contains("sous-article"))).isEmpty();
    }

    @Test
    void r22_graphiqueColonnes_categorieTranche_seriesTauxConcretisationEtLibres() throws Exception {
        String corps = corps();
        List<Map<String, Object>> graphiques = JsonPath.read(corps, "$.sections[?(@.id == 'tranches')].graphiques[*]");

        assertThat(graphiques).singleElement().satisfies(g -> assertThat(g).containsEntry("type", "COLONNES")
                .containsEntry("colonneCategorie", TRANCHE)
                .containsEntry("colonnesValeurs", List.of(TAUX_CONCRETISATION, TAUX_LIBRES)));
    }

    @Test
    void r22_descripteur_categorieChoixMultiple_trancheId_pasDeDate() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CONCRET_LIBRES)).with(libre));
        List<String> nomsParametres = JsonPath.read(corps, "$.parametres[*].nom");
        List<Map<String, Object>> categorie = JsonPath.read(corps, "$.parametres[?(@.nom == 'categorie')]");

        assertThat(nomsParametres).containsExactlyInAnyOrder("trancheId", CATEGORIE);
        assertThat(categorie).singleElement().satisfies(p -> assertThat(p).containsEntry("type", "CHOIX")
                .containsEntry("multiple", true));
    }

    @Test
    void r22_options_categoriesDArticlesVenteDeTranchesVisibles_horsCategorieDeSousArticle() throws Exception {
        StkItemType type = ev.type("TYP2-" + jeu.prefixe());
        StkItemCategory pourSous = patrimoine.categorie("CSOUS-" + jeu.prefixe(), referentielStk.natureParDefaut(), true);
        ev.article("ASOUS-" + jeu.prefixe(), t3, pourSous, type);
        StkItemCategory horsVente = jeu.categorieArticle();
        ev.article("AHV-" + jeu.prefixe(), t3, horsVente, type, ev.projetVocation());
        StkItemCategory inactive = jeu.categorieArticle();
        CfgTranche ti = jeu.tranche("TI", jeu.projet("PI", false));
        ev.article("AINA-" + jeu.prefixe(), ti, inactive, type);
        StkItemCategory sansArticle = jeu.categorieArticle();
        fixture.relire();

        String corps = appels.corps(get(descripteur(VENTE, CONCRET_LIBRES)).with(libre));
        List<String> valeurs = JsonPath.read(corps, "$.parametres[?(@.nom == 'categorie')].options[*].valeur");
        List<String> libelles = JsonPath.read(corps, "$.parametres[?(@.nom == 'categorie')].options[*].libelle");

        assertThat(valeurs).contains(catA.getId().toString(), catB.getId().toString())
                .doesNotContain(pourSous.getId().toString(), horsVente.getId().toString(),
                        inactive.getId().toString(), sansArticle.getId().toString());
        assertThat(libelles).contains(catA.getDescription());
    }

    @Test
    void r22_categorieHorsOptions_400() throws Exception {
        StkItemCategory sansArticle = jeu.categorieArticle();
        fixture.relire();

        assertThat(appels.statut(demande(CONCRET_LIBRES, libre, List.of(t1), CATEGORIE,
                sansArticle.getId().toString()))).isEqualTo(400);
    }
}
