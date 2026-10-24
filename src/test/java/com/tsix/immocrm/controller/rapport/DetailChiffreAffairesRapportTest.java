package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.services.rapport.vente.flux.AnomaliesFlux;
import com.tsix.immocrm.services.rapport.vente.flux.CleFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.SourcePrevu;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.DETAIL_CA;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.PREVU_NON_SAISI;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 7 {@code ComTdbDetailsChiffreAffaire} « DÉTAILS CHIFFRE D'AFFAIRE PAR PROJET et PAR ANNÉE » (FEATURE-tdb-vente-rapports-flux.md
 * §4.5, §9 « N° 7 », §6 {@code ValorisationTranche}). Année 2025, projet P :
 * <pre>
 *   T1 : LIB libre 400 000 + sous-article libre 40 000 ; article PROJET 999 000 (exclu)
 *        D1 MIN acte 03/2023 (1 000 000), article 1 100 000 ;  D2 MIN acte 04/2024 (900 000), article 950 000
 *        D3 MIN acte 05/2025 (700 000), article 750 000 ;     D4 ANN acte 06/2025 (2 000 000), article 600 000
 *        ⇒ caBase 3 840 000, caReel 700 000, dejaFacture 1 900 000, resteAFacturer 1 940 000
 *   T0 : sans mouvement, un article libre 300 000 ;  TI : entité inactive ;  TH : tranche hors vente (IsForSale = 0)
 * </pre>
 * {@link SourcePrevu} est simulé : vide et « non saisi » par défaut (comme {@code PrevuNonSaisi}), prévu de T1 dans un test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DetailChiffreAffairesRapportTest {

    private static final int ANNEE = 2025;
    private static final String PROJETS = "projets";
    private static final String TRANCHE = "tranche";
    private static final String CA_BASE = "caBase";
    private static final String CA_PREVISIONNEL = "caPrevisionnel";
    private static final String CA_REEL = "caReel";
    private static final String ECART = "ecart";
    private static final String DEJA_FACTURE = "dejaFacture";
    private static final String RESTE_A_FACTURER = "resteAFacturer";
    private static final String COEFFICIENT = "coefficient";
    private static final List<String> MONTANTS = List.of(CA_BASE, CA_PREVISIONNEL, CA_REEL, ECART, DEJA_FACTURE,
            RESTE_A_FACTURER);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;
    @MockitoBean private SourcePrevu sourcePrevu;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche t1;
    private CfgTranche t0;
    private CfgTranche inactive;
    private CfgTranche horsVente;

    @BeforeEach
    void setUp() {
        when(sourcePrevu.prevu(any())).thenReturn(FluxMensuel.vide(
                new AnomaliesFlux(0, null, List.of(), true)));
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        Jeu jeu = fixture.jeu("N07");
        libre = connecte(rapports.compte("n07." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        CfgProject projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        t0 = jeu.tranche("T0", projet);
        inactive = jeu.tranche("TI", jeu.projet("PI", false));
        horsVente = ev.tranche("TH-" + jeu.prefixe(), projet, false);

        StkItem lib = jeu.articleLibre(t1, "400000");
        jeu.sousArticleLibre(lib, "40000");
        jeu.articleHorsVente(t1, "999000");
        jeu.prix(jeu.dater(jeu.dossier(t1, st.minute(), "1100000"), null, LocalDate.of(2023, 3, 3), null, null),
                new BigDecimal("1000000"), null);
        jeu.prix(jeu.dater(jeu.dossier(t1, st.minute(), "950000"), null, LocalDate.of(2024, 4, 4), null, null),
                new BigDecimal("900000"), null);
        jeu.prix(jeu.dater(jeu.dossier(t1, st.minute(), "750000"), null, LocalDate.of(ANNEE, 5, 5), null, null),
                new BigDecimal("700000"), null);
        jeu.prix(jeu.dater(jeu.dossier(t1, st.annulation(), "600000"), null, LocalDate.of(ANNEE, 6, 6), null,
                LocalDate.of(ANNEE, 7, 7)), new BigDecimal("2000000"), null);
        jeu.articleLibre(t0, "300000");
        jeu.articleLibre(inactive, "5000000");
        jeu.articleLibre(horsVente, "6000000");
        fixture.relire();
    }

    private MockHttpServletRequestBuilder requete() {
        return demande(DETAIL_CA, libre, ANNEE, t1, t0, inactive, horsVente);
    }

    private Map<String, Object> projets(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), PROJETS);
    }

    private static Map<String, Object> ligneDe(Map<String, Object> s, CfgTranche t) {
        return ligne(s, TRANCHE, t.getDescription());
    }

    // §9 n° 7 : une ligne par tranche en vente visible, même sans mouvement
    @Test
    void n7_uneLigneParTrancheEnVenteVisible_memeSansMouvement_niInactiveNiHorsVente() throws Exception {
        Map<String, Object> s = projets(requete());

        assertThat(lignes(s)).extracting(l -> l.get(TRANCHE))
                .containsExactlyInAnyOrder(t1.getDescription(), t0.getDescription());
    }

    @Test
    void n7_colonnes_typesEtSommes() throws Exception {
        Map<String, Object> s = projets(requete());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(noms(s)).containsExactly("programme", TRANCHE, CA_BASE, CA_PREVISIONNEL, CA_REEL, ECART,
                DEJA_FACTURE, RESTE_A_FACTURER);
        for (String nom : MONTANTS) {
            soft.assertThat(colonne(s, nom)).as(nom).containsEntry("type", "MONTANT")
                    .containsEntry("fonctionTotal", "SOMME");
            soft.assertThat(nombre(totaux(s).get(nom))).as(nom).isEqualByComparingTo(somme(lignes(s), nom));
        }
        soft.assertAll();
    }

    // §4.5 caBase = ValorisationTranche.prixTotal : Σ SalePrice vocation VENTE, sous-articles compris
    @Test
    void n7_caBase_sommeDesPrixDeVente_sousArticlesCompris_horsAutreVocation() throws Exception {
        Map<String, Object> s = projets(requete());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligneDe(s, t1).get(CA_BASE))).isEqualByComparingTo("3840000");
        soft.assertThat(nombre(ligneDe(s, t0).get(CA_BASE))).isEqualByComparingTo("300000");
        soft.assertThat(nombre(totaux(s).get(CA_BASE))).as("tranches invisibles exclues")
                .isEqualByComparingTo("4140000");
        soft.assertAll();
    }

    @Test
    void n7_caReel_caTtcDeLAnnee_dejaFacture_anneesAnterieures_annulesExclus() throws Exception {
        Map<String, Object> l = ligneDe(projets(requete()), t1);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(CA_REEL))).isEqualByComparingTo("700000");
        soft.assertThat(nombre(l.get(DEJA_FACTURE))).isEqualByComparingTo("1900000");
        soft.assertAll();
    }

    // §9 n° 7 : resteAFacturer = caBase − dejaFacture (l'année en cours n'est pas retranchée)
    @Test
    void n7_resteAFacturer_egalCaBaseMoinsDejaFacture() throws Exception {
        Map<String, Object> s = projets(requete());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligneDe(s, t1).get(RESTE_A_FACTURER))).isEqualByComparingTo("1940000");
        soft.assertThat(nombre(ligneDe(s, t0).get(RESTE_A_FACTURER))).isEqualByComparingTo("300000");
        soft.assertAll();
    }

    // §9 n° 7 : ecart = caReel − caPrevisionnel ; prévu non saisi ⇒ 0 + avertissement
    @Test
    void n7_prevuNonSaisi_caPrevisionnelZero_ecartEgalCaReel_avertissement() throws Exception {
        String corps = appels.corps(requete());
        Map<String, Object> l = ligneDe(section(JsonPath.read(corps, "$.sections"), PROJETS), t1);
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(CA_PREVISIONNEL))).isEqualByComparingTo("0");
        soft.assertThat(nombre(l.get(ECART))).isEqualByComparingTo("700000");
        soft.assertThat(avertissements).anyMatch(a -> a.contains(PREVU_NON_SAISI))
                .as("§4.5 : le C.A. de base compte aussi les sous-articles").anyMatch(a -> a.contains("sous-articles"));
        soft.assertAll();
    }

    private void prevuDeT1() {
        FluxMensuel.Cumul cumul = new FluxMensuel.Cumul();
        cumul.ajouter(MesureFlux.PREVU_CA_TTC, new CleFlux(t1.getId(), ANNEE, 3), new BigDecimal("1000000"));
        cumul.ajouter(MesureFlux.PREVU_CA_TTC, new CleFlux(t1.getId(), ANNEE, 9), new BigDecimal("200000"));
        cumul.ajouter(MesureFlux.PREVU_CA_TTC, new CleFlux(t1.getId(), ANNEE - 1, 9), new BigDecimal("5000000"));
        when(sourcePrevu.prevu(any())).thenReturn(cumul.flux(AnomaliesFlux.AUCUNE));
    }

    // §4.5 : caPrevisionnel = PREVU_CA_TTC de l'année × coefficient ÷ 100 ; défaut 100
    @Test
    void n7_coefficientParDefaut100_caPrevisionnelEgalPrevuDeLAnnee_ecart() throws Exception {
        prevuDeT1();

        Map<String, Object> l = ligneDe(projets(requete()), t1);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(CA_PREVISIONNEL))).isEqualByComparingTo("1200000");
        soft.assertThat(nombre(l.get(ECART))).isEqualByComparingTo("-500000");
        soft.assertAll();
    }

    @Test
    void n7_coefficient50_caPrevisionnelSuit_ecartEgalCaReelMoinsCaPrevisionnel() throws Exception {
        prevuDeT1();

        Map<String, Object> l = ligneDe(projets(requete().param(COEFFICIENT, "50")), t1);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(CA_PREVISIONNEL))).isEqualByComparingTo("600000");
        soft.assertThat(nombre(l.get(ECART))).isEqualByComparingTo("100000");
        soft.assertAll();
    }

    // §4.5 : coefficient ENTIER 0–1 000, défaut 100
    @Test
    void n7_parametreCoefficient_entier_defaut100_bornes0Et1000() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, DETAIL_CA)).with(libre));
        List<Map<String, Object>> coefficient = JsonPath.read(corps, "$.parametres[?(@.nom == 'coefficient')]");

        assertThat(coefficient).singleElement().satisfies(p -> assertThat(p)
                .containsEntry("type", "ENTIER")
                .containsEntry("valeurParDefaut", "100")
                .containsEntry("min", 0)
                .containsEntry("max", 1000));
    }

    @Test
    void n7_coefficientHorsBornes_400() throws Exception {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(requete().param(COEFFICIENT, "1001"))).as("1001").isEqualTo(400);
        soft.assertThat(appels.statut(requete().param(COEFFICIENT, "-1"))).as("-1").isEqualTo(400);
        soft.assertAll();
    }
}
