package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.sousTotaux;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.SITUATION_GLOBALE;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N° 11 {@code ComTdbSalesSituationGlobal} « SITUATION GLOBALE DES VENTES » (FEATURE-tdb-vente-rapports-flux.md §4.6, §9 « N° 11 »,
 * §6.1 point 6). Projet P1 (T1, T2) et P2 (T3) ; articles VENTE à 1 000 000 de {@code SalePrice} sauf mention :
 * <pre>
 *   T1 L1  libre 300 000 + sous-article libre 30 000 ; X1 article PROJET 999 000 (exclu)
 *      R1  RES  prix 900 000, avance réglée 100 000                       ⇒ libre, reste 800 000
 *      C1  CONC concrétisé 01/2025, prix 1 100 000, avance réglée 150 000 + 50 000 CANCELED ⇒ concrétisé, reste 950 000
 *      C2  CONC sans AdvanceDate, prix 1 200 000                           ⇒ dans aucune colonne d'état
 *      V1  MIN  concrétisé + acte 2024, prix 1 000 000, crédit réglé 400 000 ⇒ vendu, reste 600 000
 *      K1  CLEFS concrétisé + acte 2023, livré 2024, prix 800 000, réglé 800 000 ⇒ vendu et livré, reste 0
 *      A1  ANN  concrétisé + acte 2023 ;  N0 sans statut, concrétisé, réglé 25 000 (hors versements)
 *   T2 un article libre 200 000 ;  T3 un article libre 100 000 ;  TI entité inactive, un article libre 9 000 000
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SituationGlobaleVentesRapportTest {

    private static final String SITUATION = "situation";
    private static final String TRANCHE = "tranche";
    private static final String PROGRAMME = "programme";
    private static final String NB_ARTICLES = "nbArticles";
    private static final String NB_LIBRES = "nbLibres";
    private static final String NB_CONCRETISES = "nbConcretises";
    private static final String NB_VENDUS = "nbVendus";
    private static final String NB_LIVRES = "nbLivres";
    private static final String VALORISATION = "valorisationStock";
    private static final String CA_LIBRE = "caLibre";
    private static final String CA_CONCRETISE = "caConcretise";
    private static final String CA_VENDU = "caVendu";
    private static final String RESTE_LIBRES = "resteAEncaisserLibres";
    private static final String RELIQUAT = "reliquatConcretises";
    private static final String RESTE = "resteAEncaisser";
    private static final String VERSEMENTS = "versements";
    private static final List<String> CHIFFRES = List.of(NB_ARTICLES, NB_LIBRES, NB_CONCRETISES, NB_VENDUS, NB_LIVRES,
            VALORISATION, CA_LIBRE, CA_CONCRETISE, CA_VENDU, RESTE_LIBRES, RELIQUAT, RESTE, VERSEMENTS);
    private static final String MILLION = "1000000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgProject p1;
    private CfgProject p2;
    private CfgTranche t1;
    private CfgTranche t2;
    private CfgTranche t3;
    private CfgTranche inactive;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        Jeu jeu = fixture.jeu("N11");
        libre = connecte(rapports.compte("n11." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        p1 = jeu.projet("P1", true);
        p2 = jeu.projet("P2", true);
        t1 = jeu.tranche("T1", p1);
        t2 = jeu.tranche("T2", p1);
        t3 = jeu.tranche("T3", p2);
        inactive = jeu.tranche("TI", jeu.projet("PI", false));

        StkItem l1 = jeu.articleLibre(t1, "300000");
        jeu.sousArticleLibre(l1, "30000");
        jeu.articleHorsVente(t1, "999000");

        ComFolder r1 = jeu.prix(jeu.dossier(t1, st.reservation(), MILLION), new BigDecimal("900000"), null);
        regler(jeu, r1, Genre.AVANCE, "100000", IN_WORK, LocalDate.of(2024, 1, 10));

        ComFolder c1 = jeu.prix(jeu.dater(jeu.dossier(t1, st.concretisation(), MILLION), LocalDate.of(2025, 1, 5), null,
                null, null), new BigDecimal("1100000"), null);
        regler(jeu, c1, Genre.AVANCE, "150000", IN_WORK, LocalDate.of(2025, 1, 6));
        regler(jeu, c1, Genre.AVANCE, "50000", CANCELED, LocalDate.of(2025, 1, 7));

        jeu.prix(jeu.dossier(t1, st.concretisation(), MILLION), new BigDecimal("1200000"), null);

        ComFolder v1 = jeu.prix(jeu.dater(jeu.dossier(t1, st.minute(), MILLION), LocalDate.of(2024, 2, 2),
                LocalDate.of(2024, 5, 5), null, null), new BigDecimal(MILLION), null);
        regler(jeu, v1, Genre.CREDIT, "400000", IN_WORK, LocalDate.of(2024, 6, 6));

        ComFolder k1 = jeu.prix(jeu.dater(jeu.dossier(t1, st.remiseClefs(), MILLION), LocalDate.of(2023, 2, 2),
                LocalDate.of(2023, 5, 5), LocalDate.of(2024, 3, 3), null), new BigDecimal("800000"), null);
        regler(jeu, k1, Genre.AUTOFINANCEMENT, "800000", IN_WORK, LocalDate.of(2023, 6, 6));

        jeu.dater(jeu.dossier(t1, st.annulation(), MILLION), LocalDate.of(2023, 3, 3), LocalDate.of(2023, 4, 4), null,
                LocalDate.of(2024, 1, 1));
        ComFolder n0 = jeu.dater(jeu.dossier(t1, null, MILLION), LocalDate.of(2024, 3, 3), null, null, null);
        regler(jeu, n0, Genre.AVANCE, "25000", IN_WORK, LocalDate.of(2024, 3, 4));

        jeu.articleLibre(t2, "200000");
        jeu.articleLibre(t3, "100000");
        jeu.articleLibre(inactive, "9000000");
        fixture.relire();
    }

    /** Échéance au crédit de la nature {@code genre}, réglée du même montant à la date donnée. */
    private static void regler(Jeu jeu, ComFolder dossier, Genre genre, String montant, String statut, LocalDate date) {
        BigDecimal valeur = new BigDecimal(montant);
        jeu.regler(jeu.echeance(dossier, jeu.categorie(genre, true), valeur, date), valeur, true, statut, null, date);
    }

    private MockHttpServletRequestBuilder requete() {
        return demande(SITUATION_GLOBALE, libre, null, t1, t2, t3, inactive);
    }

    private Map<String, Object> situation() throws Exception {
        return section(appels.sections(requete()), SITUATION);
    }

    private static Map<String, Object> ligneDe(Map<String, Object> s, CfgTranche t) {
        return ligne(s, TRANCHE, t.getDescription());
    }

    @Test
    void n11_uneLigneParTrancheVisible_entiteInactiveAbsente() throws Exception {
        Map<String, Object> s = situation();

        assertThat(lignes(s)).extracting(l -> l.get(TRANCHE))
                .containsExactlyInAnyOrder(t1.getDescription(), t2.getDescription(), t3.getDescription());
    }

    @Test
    void n11_nombres_articlesRacinesVente_libresPlusReserves_concretisesVendusLivres() throws Exception {
        Map<String, Object> l = ligneDe(situation(), t1);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(NB_ARTICLES))).as("L1 R1 C1 C2 V1 K1 A1 N0").isEqualByComparingTo("8");
        soft.assertThat(nombre(l.get(NB_LIBRES))).as("L1 + R1 réservé").isEqualByComparingTo("2");
        soft.assertThat(nombre(l.get(NB_CONCRETISES))).as("C1 seul (C2 sans AdvanceDate)").isEqualByComparingTo("1");
        soft.assertThat(nombre(l.get(NB_VENDUS))).as("V1, K1 ; A1 annulé exclu").isEqualByComparingTo("2");
        soft.assertThat(nombre(l.get(NB_LIVRES))).as("K1").isEqualByComparingTo("1");
        soft.assertAll();
    }

    // §9 n° 11 : nbConcretises + nbVendus ≤ nbArticles − nbLibres
    @Test
    void n11_concretisesPlusVendus_auPlusArticlesMoinsLibres() throws Exception {
        List<Map<String, Object>> l = lignes(situation());

        assertThat(l).isNotEmpty().allSatisfy(ligne -> assertThat(nombre(ligne.get(NB_CONCRETISES))
                .add(nombre(ligne.get(NB_VENDUS))))
                .isLessThanOrEqualTo(nombre(ligne.get(NB_ARTICLES)).subtract(nombre(ligne.get(NB_LIBRES)))));
    }

    @Test
    void n11_valorisationEtChiffreDAffaires_parPopulation() throws Exception {
        Map<String, Object> l = ligneDe(situation(), t1);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(VALORISATION))).as("Σ SalePrice VENTE, sous-articles compris")
                .isEqualByComparingTo("7330000");
        soft.assertThat(nombre(l.get(CA_LIBRE))).as("330 000 libres + 900 000 prix du réservé")
                .isEqualByComparingTo("1230000");
        soft.assertThat(nombre(l.get(CA_CONCRETISE))).isEqualByComparingTo("1100000");
        soft.assertThat(nombre(l.get(CA_VENDU))).isEqualByComparingTo("1800000");
        soft.assertAll();
    }

    @Test
    void n11_tresorerie_resteARegler_reglementAnnuleIgnore_sansBorneHaute() throws Exception {
        Map<String, Object> l = ligneDe(situation(), t1);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(RESTE_LIBRES))).as("330 000 + reste du réservé 800 000")
                .isEqualByComparingTo("1130000");
        soft.assertThat(nombre(l.get(RELIQUAT))).as("C1 950 000 + V1 600 000 + K1 0")
                .isEqualByComparingTo("1550000");
        soft.assertThat(nombre(l.get(RESTE))).isEqualByComparingTo("2680000");
        soft.assertAll();
    }

    // §4.6 + R-F5 : versements = ENCAISSEMENTS de la tranche toutes années ; dossier sans statut exclu
    @Test
    void n11_versements_encaissementsToutesAnnees_annuleEtSansStatutExclus() throws Exception {
        Map<String, Object> s = situation();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligneDe(s, t1).get(VERSEMENTS))).as("100 000 + 150 000 + 400 000 + 800 000")
                .isEqualByComparingTo("1450000");
        soft.assertThat(nombre(ligneDe(s, t2).get(VERSEMENTS))).isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void n11_groupeParProgramme_sousTotalEgalSommeDeSesTranches() throws Exception {
        Map<String, Object> s = situation();
        Map<String, Map<String, Object>> parProgramme = sousTotaux(s);

        assertThat(s).containsEntry("groupePar", PROGRAMME);
        assertThat(parProgramme).isNotNull().containsKeys(p1.getDescription(), p2.getDescription());
        SoftAssertions soft = new SoftAssertions();
        for (String nom : CHIFFRES) {
            soft.assertThat(nombre(parProgramme.get(p1.getDescription()).get(nom))).as("P1 %s", nom)
                    .isEqualByComparingTo(nombre(ligneDe(s, t1).get(nom)).add(nombre(ligneDe(s, t2).get(nom))));
            soft.assertThat(nombre(parProgramme.get(p2.getDescription()).get(nom))).as("P2 %s", nom)
                    .isEqualByComparingTo(nombre(ligneDe(s, t3).get(nom)));
        }
        soft.assertAll();
    }

    @Test
    void n11_totalGeneral_egalSommeDesSousTotaux_etDesLignes() throws Exception {
        Map<String, Object> s = situation();
        Map<String, Map<String, Object>> parProgramme = sousTotaux(s);

        assertThat(parProgramme).isNotNull().isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        for (String nom : CHIFFRES) {
            BigDecimal sousTotaux = parProgramme.values().stream().map(v -> nombre(v.get(nom)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry("fonctionTotal", "SOMME");
            soft.assertThat(nombre(totaux(s).get(nom))).as(nom).isEqualByComparingTo(sousTotaux)
                    .isEqualByComparingTo(somme(lignes(s), nom));
        }
        soft.assertThat(nombre(totaux(s).get(VALORISATION))).as("sans l'entité inactive")
                .isEqualByComparingTo("7630000");
        soft.assertAll();
    }

    @Test
    void n11_groupesDEnTete_duModele() throws Exception {
        Map<String, Object> s = situation();

        SoftAssertions soft = new SoftAssertions();
        for (String nom : List.of(NB_ARTICLES, NB_LIBRES, NB_CONCRETISES, NB_VENDUS, NB_LIVRES)) {
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry("groupe", "Nombre d'articles");
        }
        soft.assertThat(colonne(s, VALORISATION)).containsEntry("groupe", "Valorisation");
        for (String nom : List.of(CA_LIBRE, CA_CONCRETISE, CA_VENDU)) {
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry("groupe", "Chiffre d'affaires TTC");
        }
        for (String nom : List.of(RESTE_LIBRES, RELIQUAT, RESTE, VERSEMENTS)) {
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry("groupe", "Trésorerie");
        }
        soft.assertAll();
    }

    // §2 (d) : paramètre de statut absent ⇒ jamais d'exception, avertissement qui nomme le paramètre
    @Test
    void n11_parametreReservationAbsent_200_avertissementNommantLeParametre() throws Exception {
        fixture.retirerParametre(FixtureFluxVente.SEUIL_RESERVATION);
        fixture.relire();

        List<String> avertissements = JsonPath.read(appels.corps(requete()), "$.avertissements");

        assertThat(avertissements).anyMatch(a -> a.contains(FixtureFluxVente.SEUIL_RESERVATION));
    }
}
