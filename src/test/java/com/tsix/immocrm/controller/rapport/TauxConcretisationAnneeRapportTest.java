package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.sousTotaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TAUX_CONCRET_ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ligneContenant;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.lignesContenant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 27 {@code ComSaleTauxConcretisationParAnnee} « TAUX DE CONCRÉTISATION PAR ANNÉE / TRANCHE » (FEATURE-tdb-vente-rapports-
 * rythme.md §4.8, §7 « N° 27 », décision du manager 25/10 : sous-article = {@code IsUnderItem}). Projet P actif :
 * <pre>
 *   T1 : d1 CONC avancé 10/09/2070 · d2 CONC 05/02/2071 · d3 CONC 20/03/2071 · d4 ANN avancé 01/01/2070 (non net)
 *        dossier CONC sur sous-article avancé 01/06/2069 (ignoré ; son parent racine compte) · 2 libres
 *        ⇒ totalArticles 7 ; 2070 : 1 (14,29 %), 2071 : 2 (28,57 %), Reste 4 (57,14 %)
 *   T2 : 2 libres, aucune concrétisation ⇒ Reste 2 (100 %)
 *   T3 : aucun article
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TauxConcretisationAnneeRapportTest {

    private static final String TRANCHES = "tranches";
    private static final String TAUX = "taux";
    private static final String TRANCHE = "tranche";
    private static final String ANNEE = "annee";
    private static final String TOTAL_ARTICLES = "totalArticles";
    private static final String PREMIERE = "premiereConcretisation";
    private static final String NB_ARTICLES = "nbArticles";
    private static final String RESTE = "Reste";
    private static final String PRIX = "1000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureRapportsPatrimoine rp;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private Jeu jeu;
    private Statuts st;
    private CfgProject projet;
    private CfgTranche t1;
    private CfgTranche t2;
    private CfgTranche t3;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsRythmeSupport.installerR2b(rapports);
        jeu = fixture.jeu("TC27");
        libre = connecte(rapports.compte("tc27." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        st = jeu.statuts();
        projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        t2 = jeu.tranche("T2", projet);
        t3 = jeu.tranche("T3", projet);

        avancer(jeu.dossier(t1, st.concretisation()), LocalDate.of(2070, 9, 10));
        avancer(jeu.dossier(t1, st.concretisation()), LocalDate.of(2071, 2, 5));
        avancer(jeu.dossier(t1, st.concretisation()), LocalDate.of(2071, 3, 20));
        avancer(jeu.dossier(t1, st.annulation()), LocalDate.of(2070, 1, 1));
        avancer(jeu.dossierSousArticle(t1, st.concretisation()), LocalDate.of(2069, 6, 1));
        jeu.articleLibre(t1, PRIX);
        jeu.articleLibre(t1, PRIX);
        jeu.articleLibre(t2, PRIX);
        jeu.articleLibre(t2, PRIX);
        fixture.relire();
    }

    private ComFolder avancer(ComFolder dossier, LocalDate avance) {
        return jeu.dater(dossier, avance, null, null, null);
    }

    private String corps(CfgTranche... tranches) throws Exception {
        return appels.corps(demande(TAUX_CONCRET_ANNEE, libre, List.of(tranches)));
    }

    private List<Map<String, Object>> sections(CfgTranche... tranches) throws Exception {
        return JsonPath.read(corps(tranches), "$.sections");
    }

    private List<Map<String, Object>> sections() throws Exception {
        return sections(t1, t2, t3);
    }

    /** Lignes de la section {@code taux} de la tranche {@code t}, « annee / nbArticles / taux ». */
    private static List<String> tauxDe(Map<String, Object> taux, CfgTranche t) {
        return lignesContenant(taux, TRANCHE, t.getCode()).stream().map(l -> l.get(ANNEE) + " / "
                + nombre(l.get(NB_ARTICLES)).stripTrailingZeros().toPlainString() + " / "
                + nombre(l.get(TAUX)).setScale(2, RoundingMode.HALF_UP).toPlainString()).toList();
    }

    // ------------------------------------------------------------------ structure (§4.8)

    @Test
    void r27_deuxSections_tranchesEtTaux_colonnes_groupeParTranche() throws Exception {
        List<Map<String, Object>> sections = sections();

        assertThat(sections).extracting(s -> s.get("id")).containsExactly(TRANCHES, TAUX);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(noms(section(sections, TRANCHES))).containsExactly(TRANCHE, TOTAL_ARTICLES, PREMIERE);
        soft.assertThat(noms(section(sections, TAUX))).containsExactly(TRANCHE, ANNEE, NB_ARTICLES, TAUX);
        soft.assertThat(section(sections, TAUX)).containsEntry("groupePar", TRANCHE);
        soft.assertThat(colonne(section(sections, TAUX), ANNEE).get("type")).isEqualTo("TEXTE");
        soft.assertThat(colonne(section(sections, TAUX), NB_ARTICLES).get("type")).isEqualTo("NOMBRE");
        soft.assertThat(colonne(section(sections, TAUX), TAUX).get("type")).isEqualTo("POURCENTAGE");
        soft.assertThat(colonne(section(sections, TRANCHES), PREMIERE).get("type")).isEqualTo("TEXTE");
        soft.assertAll();
    }

    @Test
    void r27_tranches_uneLigneParTrancheAvecArticle_descriptionEtTotalArticles_trancheSansArticleAbsente()
            throws Exception {
        Map<String, Object> s = section(sections(), TRANCHES);

        assertThat(lignes(s)).hasSize(2);
        assertThat(lignesContenant(s, TRANCHE, t3.getCode())).isEmpty();
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(ligneContenant(s, TRANCHE, t1.getCode())).containsEntry(TRANCHE, t1.getDescription());
        soft.assertThat(nombre(ligneContenant(s, TRANCHE, t1.getCode()).get(TOTAL_ARTICLES))).as("racines VENTE")
                .isEqualByComparingTo("7");
        soft.assertThat(nombre(ligneContenant(s, TRANCHE, t2.getCode()).get(TOTAL_ARTICLES))).isEqualByComparingTo("2");
        soft.assertAll();
    }

    @Test
    void r27_premiereConcretisation_minDesDatesNettesSurRacine_videSansConcretisation() throws Exception {
        Map<String, Object> s = section(sections(), TRANCHES);

        Object deT2 = ligneContenant(s, TRANCHE, t2.getCode()).get(PREMIERE);
        assertThat(ligneContenant(s, TRANCHE, t1.getCode())).as("ni le sous-article de 2069 ni l'annulé de janv. 2070")
                .containsEntry(PREMIERE, "Septembre 2070");
        assertThat(deT2 == null || String.valueOf(deT2).isBlank()).as("T2 : %s", deT2).isTrue();
    }

    // ------------------------------------------------------------------ taux par année (§7 « N° 27 »)

    @Test
    void r27_taux_anneesTrieesPuisReste_nettesSurRacineSeulement() throws Exception {
        Map<String, Object> taux = section(sections(), TAUX);

        assertThat(tauxDe(taux, t1)).as("sous-article 2069 et annulé 2070 ignorés")
                .containsExactly("2070 / 1 / 14.29", "2071 / 2 / 28.57", "Reste / 4 / 57.14");
        assertThat(tauxDe(taux, t2)).containsExactly("Reste / 2 / 100.00");
    }

    @Test
    void r27_sousTotalParTranche_egalTotalArticles_cent() throws Exception {
        Map<String, Object> taux = section(sections(), TAUX);
        Map<String, Map<String, Object>> parTranche = sousTotaux(taux);

        assertThat(parTranche).isNotNull();
        List<String> cles = parTranche.keySet().stream().filter(k -> k.contains(t1.getCode())).toList();
        assertThat(cles).hasSize(1);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(parTranche.get(cles.get(0)).get(NB_ARTICLES))).as("Σ années + reste = totalArticles")
                .isEqualByComparingTo("7");
        soft.assertThat(nombre(parTranche.get(cles.get(0)).get(TAUX))).isEqualByComparingTo("100");
        soft.assertAll();
    }

    @Test
    void r27_resteNegatif_borneAZero_avertissement() throws Exception {
        CfgTranche t4 = jeu.tranche("T4", projet);
        jeu.articleLibre(t4, PRIX);
        for (int mois = 1; mois <= 2; mois++) {
            StkItem horsVente = jeu.articleHorsVente(t4, PRIX);
            avancer(ev.vendre(horsVente, "DOS-HV" + mois + "-" + jeu.prefixe(), st.concretisation(), null, null),
                    LocalDate.of(2072, mois, 10));
        }
        fixture.relire();

        String corps = corps(t4);
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");

        Map<String, Object> reste = ligneContenant(section(sections, TAUX), ANNEE, RESTE);
        assertThat(nombre(reste.get(NB_ARTICLES))).as("1 article, 2 concrétisations").isEqualByComparingTo("0");
        assertThat(avertissements).isNotEmpty();
    }

    @Test
    void r27_articleSansLienRetourVersLeDossier_concretisationCompteeParLeFlux() throws Exception {
        CfgTranche t5 = jeu.tranche("T5", projet);
        ComFolder d = avancer(jeu.dossier(t5, st.concretisation()), LocalDate.of(2072, 3, 1));
        StkItem article = d.getStkItem();
        article.setComFolderId(null);
        rp.enregistrer(article);
        fixture.relire();

        Map<String, Object> taux = section(sections(t5), TAUX);

        assertThat(tauxDe(taux, t5)).containsExactly("2072 / 1 / 100.00", "Reste / 0 / 0.00");
    }

    // ------------------------------------------------------------------ graphique et paramètres

    @Test
    void r27_graphiqueColonnes_categorieAnnee_serieNbArticles() throws Exception {
        List<Map<String, Object>> graphiques = JsonPath.read(corps(t1, t2, t3), "$.sections[*].graphiques[*]");

        assertThat(graphiques).singleElement().satisfies(g -> assertThat(g).containsEntry("type", "COLONNES")
                .containsEntry("colonneCategorie", ANNEE).containsEntry("colonnesValeurs", List.of(NB_ARTICLES)));
    }

    @Test
    void r27_descripteur_seulementTrancheId() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, TAUX_CONCRET_ANNEE)).with(libre));
        List<String> nomsParametres = JsonPath.read(corps, "$.parametres[*].nom");

        assertThat(nomsParametres).containsExactly("trancheId");
    }
}
