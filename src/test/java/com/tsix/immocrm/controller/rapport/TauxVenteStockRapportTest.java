package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import com.tsix.immocrm.services.rapport.stock.MouvementsStockParReflexion;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
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
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TAUX_VENTE_STOCK;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ligneContenant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 24 {@code ComTdbSaleStockRatio} « TAUX DE VENTE PAR RAPPORT AU STOCK » (FEATURE-tdb-vente-rapports-rythme.md §4.7, §3.5,
 * §7 « N° 24 ») ; N = 2071. Source unique : {@code MouvementsStockTranche}. Projet P actif ; seuils de désistement et
 * d'annulation = statuts DES et ANN du jeu :
 * <pre>
 *   TA (lancée 15/03/2070, 10 racines VENTE) : a1 avancé 05/05/2070 · a2 avancé 10/06/2070, désisté 20/02/2071
 *       a3 15/01/2071 · a4 10/02/2071 · a5 15/02/2071 · a6 avancé 01/03/2071, annulé 10/04/2071 · 4 libres
 *   TB (lancée 01/02/2071, 5 racines) : b1 avancé 20/02/2071 · 4 libres
 *   TC (en vente, SANS MarketingStartDate) : 1 libre
 *   ⇒ 2070 : stock initial 10, ventes 2 (20 %) ; 2071 : stock initial 8 + 5 = 13, ventes 4 + 1 − 2 retours = 3 (23,08 %)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TauxVenteStockRapportTest {

    private static final int N = 2071;
    private static final String MOIS = "mois";
    private static final String TRANCHES = "tranches";
    private static final String ANNEES = "annees";
    private static final String CUMULS = "cumuls";
    private static final String DEPART_N1 = "departAnneeMoins1";
    private static final String VENTES_N1 = "ventesAnneeMoins1";
    private static final String DEPART_N = "departAnnee";
    private static final String VENTES_N = "ventesAnnee";
    private static final String PERF_N1 = "perfAnneeMoins1";
    private static final String PERF_N = "perfAnnee";
    private static final String CUMUL_N1 = "cumulVentesAnneeMoins1";
    private static final String CUMUL_N = "cumulVentesAnnee";
    private static final String STOCK_INITIAL = "stockInitial";
    private static final String VENTES = "ventes";
    private static final String TAUX = "taux";
    private static final String TRANCHE = "tranche";
    private static final String PRIX = "1000";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationContext contexte;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche ta;
    private CfgTranche tb;
    private CfgTranche tc;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsRythmeSupport.installerR2b(rapports);
        Jeu jeu = fixture.jeu("TV24");
        libre = connecte(rapports.compte("tv24." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_RESILIATION, st.desistement().getId());
        fixture.parametre(FixtureRapportsPatrimoine.SEUIL_ANNULATION, st.annulation().getId());
        CfgProject projet = jeu.projet("P", true);
        ta = jeu.tranche("TA", projet);
        tb = jeu.tranche("TB", projet);
        tc = jeu.tranche("TC", projet);

        avancer(jeu, ta, st.concretisation(), j(2070, 5, 5), null);
        avancer(jeu, ta, st.desistement(), j(2070, 6, 10), j(N, 2, 20));
        avancer(jeu, ta, st.concretisation(), j(N, 1, 15), null);
        avancer(jeu, ta, st.concretisation(), j(N, 2, 10), null);
        avancer(jeu, ta, st.concretisation(), j(N, 2, 15), null);
        avancer(jeu, ta, st.annulation(), j(N, 3, 1), j(N, 4, 10));
        libres(jeu, ta, 4);
        ta = jeu.lancer(ta, j(2070, 3, 15));

        avancer(jeu, tb, st.concretisation(), j(N, 2, 20), null);
        libres(jeu, tb, 4);
        tb = jeu.lancer(tb, j(N, 2, 1));

        libres(jeu, tc, 1);
        fixture.relire();
    }

    private static void avancer(Jeu jeu, CfgTranche tranche, StrStatus statut, LocalDate avance, LocalDate sortie) {
        jeu.dater(jeu.dossier(tranche, statut), avance, null, null, sortie);
    }

    private static void libres(Jeu jeu, CfgTranche tranche, int nombre) {
        for (int i = 0; i < nombre; i++) {
            jeu.articleLibre(tranche, PRIX);
        }
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private String corps() throws Exception {
        return appels.corps(demande(TAUX_VENTE_STOCK, libre, List.of(ta, tb, tc), ANNEE, String.valueOf(N)));
    }

    private List<Map<String, Object>> sections() throws Exception {
        return JsonPath.read(corps(), "$.sections");
    }

    /** Ligne du mois {@code m} (1–12) de la section {@code mois} ou {@code cumuls}. */
    private static Map<String, Object> mois(Map<String, Object> section, int m) {
        assertThat(lignes(section)).as(String.valueOf(section.get("id"))).hasSize(12);
        return lignes(section).get(m - 1);
    }

    private static BigDecimal valeur(Map<String, Object> ligne, String colonne) {
        return nombre(ligne.get(colonne));
    }

    // ------------------------------------------------------------------ structure (§4.7)

    @Test
    void r24_quatreSections_dansLOrdre_colonnes() throws Exception {
        List<Map<String, Object>> sections = sections();

        assertThat(sections).extracting(s -> s.get("id")).containsExactly(MOIS, TRANCHES, ANNEES, CUMULS);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(noms(section(sections, MOIS))).containsSubsequence(MOIS, DEPART_N1, VENTES_N1, DEPART_N,
                VENTES_N).contains(PERF_N1, PERF_N, "totalVentes");
        soft.assertThat(noms(section(sections, TRANCHES))).containsExactly(TRANCHE, "stockInitialAnneeMoins1", VENTES_N1,
                "stockInitialAnnee", VENTES_N);
        soft.assertThat(noms(section(sections, ANNEES))).containsExactly(ANNEE, STOCK_INITIAL, VENTES, TAUX);
        soft.assertThat(noms(section(sections, CUMULS))).containsExactly(MOIS, CUMUL_N1, CUMUL_N);
        soft.assertThat(colonne(section(sections, MOIS), PERF_N).get("type")).isEqualTo("POURCENTAGE");
        soft.assertThat(colonne(section(sections, MOIS), VENTES_N).get("type")).isEqualTo("NOMBRE");
        soft.assertThat(colonne(section(sections, ANNEES), TAUX).get("type")).isEqualTo("POURCENTAGE");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ stock de départ et ventes (§7 « N° 24 »)

    @Test
    void r24_departDeJanvierN_mouvementsAvantLe01_01_plusLancementsDeJanvier() throws Exception {
        Map<String, Object> s = section(sections(), MOIS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(mois(s, 1), DEPART_N)).as("10 − 2").isEqualByComparingTo("8");
        soft.assertThat(valeur(mois(s, 2), DEPART_N)).as("8 − 1 + lancement de TB en février (5)")
                .isEqualByComparingTo("12");
        soft.assertThat(valeur(mois(s, 3), DEPART_N)).isEqualByComparingTo("10");
        soft.assertThat(valeur(mois(s, 4), DEPART_N)).isEqualByComparingTo("9");
        soft.assertThat(valeur(mois(s, 3), DEPART_N1)).as("lancement de TA en mars 2070").isEqualByComparingTo("10");
        soft.assertThat(valeur(mois(s, 1), DEPART_N1)).isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r24_vente_moinsSommeDesMouvementsHorsLancement_desistementDeLAnneeRetireUneVente() throws Exception {
        Map<String, Object> s = section(sections(), MOIS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(mois(s, 1), VENTES_N)).isEqualByComparingTo("1");
        soft.assertThat(valeur(mois(s, 2), VENTES_N)).as("a4 a5 b1 − désistement de a2").isEqualByComparingTo("2");
        soft.assertThat(valeur(mois(s, 3), VENTES_N)).isEqualByComparingTo("1");
        soft.assertThat(valeur(mois(s, 4), VENTES_N)).as("annulation de a6").isEqualByComparingTo("-1");
        soft.assertThat(valeur(mois(s, 5), VENTES_N1)).isEqualByComparingTo("1");
        soft.assertThat(valeur(mois(s, 6), VENTES_N1)).isEqualByComparingTo("1");
        soft.assertThat(valeur(mois(s, 3), VENTES_N1)).as("le lancement n'est pas une vente").isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r24_perf_ventesSurDepartDuMois_departNulDonneZero() throws Exception {
        Map<String, Object> s = section(sections(), MOIS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(mois(s, 1), PERF_N)).as("1 ÷ 8").isEqualByComparingTo("12.50");
        soft.assertThat(valeur(mois(s, 2), PERF_N)).as("2 ÷ 12").isEqualByComparingTo("16.67");
        soft.assertThat(valeur(mois(s, 6), PERF_N1)).as("1 ÷ 9").isEqualByComparingTo("11.11");
        soft.assertThat(valeur(mois(s, 1), PERF_N1)).as("départ 0").isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r24_totauxDuMois_sommeDesVentes() throws Exception {
        Map<String, Object> t = totaux(section(sections(), MOIS));

        assertThat(valeur(t, VENTES_N)).isEqualByComparingTo("3");
        assertThat(valeur(t, VENTES_N1)).isEqualByComparingTo("2");
    }

    @Test
    void r24_tranches_stockInitialEtVentesParAnnee_uneLigneParTrancheVisible() throws Exception {
        Map<String, Object> s = section(sections(), TRANCHES);

        assertThat(lignes(s)).hasSize(3);
        SoftAssertions soft = new SoftAssertions();
        for (Object[] attendu : new Object[][]{{ta, "10", "2", "8", "2"}, {tb, "0", "0", "5", "1"},
                {tc, "0", "0", "0", "0"}}) {
            CfgTranche t = (CfgTranche) attendu[0];
            Map<String, Object> l = ligneContenant(s, TRANCHE, t.getCode());
            soft.assertThat(valeur(l, "stockInitialAnneeMoins1")).as(t.getCode()).isEqualByComparingTo((String) attendu[1]);
            soft.assertThat(valeur(l, VENTES_N1)).as(t.getCode()).isEqualByComparingTo((String) attendu[2]);
            soft.assertThat(valeur(l, "stockInitialAnnee")).as(t.getCode()).isEqualByComparingTo((String) attendu[3]);
            soft.assertThat(valeur(l, VENTES_N)).as(t.getCode()).isEqualByComparingTo((String) attendu[4]);
        }
        soft.assertThat(valeur(totaux(s), VENTES_N)).isEqualByComparingTo("3");
        soft.assertAll();
    }

    @Test
    void r24_annees_tauxImposeSommeDesVentesSurSommeDesStocksInitiaux() throws Exception {
        Map<String, Object> s = section(sections(), ANNEES);

        assertThat(lignes(s)).extracting(l -> String.valueOf(l.get(ANNEE)))
                .containsExactly(String.valueOf(N - 1), String.valueOf(N));
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(valeur(lignes(s).get(0), STOCK_INITIAL)).isEqualByComparingTo("10");
        soft.assertThat(valeur(lignes(s).get(0), VENTES)).isEqualByComparingTo("2");
        soft.assertThat(valeur(lignes(s).get(0), TAUX)).isEqualByComparingTo("20");
        soft.assertThat(valeur(lignes(s).get(1), STOCK_INITIAL)).as("8 + lancement TB 5").isEqualByComparingTo("13");
        soft.assertThat(valeur(lignes(s).get(1), VENTES)).isEqualByComparingTo("3");
        soft.assertThat(valeur(lignes(s).get(1), TAUX)).as("3 ÷ 13").isEqualByComparingTo("23.08");
        soft.assertAll();
    }

    @Test
    void r24_cumuls_sommeCouranteDesVentesDuMois() throws Exception {
        Map<String, Object> s = section(sections(), CUMULS);

        List<String> attendusN = List.of("1", "3", "4", "3", "3", "3", "3", "3", "3", "3", "3", "3");
        List<String> attendusN1 = List.of("0", "0", "0", "0", "1", "2", "2", "2", "2", "2", "2", "2");
        SoftAssertions soft = new SoftAssertions();
        for (int m = 1; m <= 12; m++) {
            soft.assertThat(valeur(mois(s, m), CUMUL_N)).as("N mois %d", m).isEqualByComparingTo(attendusN.get(m - 1));
            soft.assertThat(valeur(mois(s, m), CUMUL_N1)).as("N−1 mois %d", m)
                    .isEqualByComparingTo(attendusN1.get(m - 1));
        }
        soft.assertAll();
    }

    /** Source unique (§3.5) : les ventes de N = − Σ des mouvements hors lancement lus par le composant partagé. */
    @Test
    void r24_ventesDeLAnnee_egalMoinsSommeDesMouvementsDuComposantPartage() throws Exception {
        long attendu = -MouvementsStockParReflexion.lire(contexte, List.of(ta.getId(), tb.getId(), tc.getId())).stream()
                .filter(m -> !MouvementsStockParReflexion.LANCEMENT.equals(m.libelle()) && m.date().getYear() == N)
                .mapToLong(MouvementsStockParReflexion.Ligne::quantite).sum();

        Map<String, Object> annees = section(sections(), ANNEES);

        assertThat(valeur(lignes(annees).get(1), VENTES)).isEqualByComparingTo(String.valueOf(attendu))
                .isEqualByComparingTo("3");
    }

    // ------------------------------------------------------------------ graphiques, avertissement, paramètres

    @Test
    void r24_graphiques_colonnesMois_lignesCumuls_colonnesAnnees() throws Exception {
        String corps = corps();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(graphiques(corps, MOIS)).singleElement().satisfies(g -> assertThat(g)
                .containsEntry("type", "COLONNES").containsEntry("colonneCategorie", MOIS)
                .containsEntry("colonnesValeurs", List.of(VENTES_N1, VENTES_N)));
        soft.assertThat(graphiques(corps, CUMULS)).singleElement().satisfies(g -> assertThat(g)
                .containsEntry("type", "LIGNES").containsEntry("colonnesValeurs", List.of(CUMUL_N1, CUMUL_N)));
        soft.assertThat(graphiques(corps, ANNEES)).singleElement().satisfies(g -> assertThat(g)
                .containsEntry("type", "COLONNES").containsEntry("colonnesValeurs", List.of(VENTES)));
        soft.assertAll();
    }

    private static List<Map<String, Object>> graphiques(String corps, String section) {
        return JsonPath.read(corps, "$.sections[?(@.id == '" + section + "')].graphiques[*]");
    }

    @Test
    void r24_trancheVisibleSansDateDeLancement_avertissementQuiLaNomme() throws Exception {
        List<String> avertissements = JsonPath.read(corps(), "$.avertissements");
        String sansTc = appels.corps(demande(TAUX_VENTE_STOCK, libre, List.of(ta, tb), ANNEE, String.valueOf(N)));
        List<String> avertissementsSansTc = JsonPath.read(sansTc, "$.avertissements");

        assertThat(avertissements).anyMatch(a -> a.contains(tc.getCode()));
        assertThat(avertissementsSansTc.stream().filter(a -> a.contains(ta.getCode()) || a.contains(tb.getCode())))
                .isEmpty();
    }

    @Test
    void r24_descripteur_anneeDefautAnneeCourante_trancheId() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, TAUX_VENTE_STOCK)).with(libre));
        List<Map<String, Object>> annee = JsonPath.read(corps, "$.parametres[?(@.nom == 'annee')]");
        List<String> nomsParametres = JsonPath.read(corps, "$.parametres[*].nom");

        assertThat(nomsParametres).containsExactlyInAnyOrder(ANNEE, "trancheId");
        assertThat(annee).singleElement().satisfies(p -> assertThat(p).containsEntry("type", "ANNEE")
                .containsEntry("valeurParDefaut", String.valueOf(LocalDate.now(clock).getYear())));
    }
}
