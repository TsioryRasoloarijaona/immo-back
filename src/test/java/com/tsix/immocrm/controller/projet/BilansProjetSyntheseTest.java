package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsBilans.PROJETS;
import static com.tsix.immocrm.controller.projet.AppelsBilans.TTC;
import static com.tsix.immocrm.controller.projet.AppelsBilans.champ;
import static com.tsix.immocrm.controller.projet.AppelsBilans.occurrences;
import static com.tsix.immocrm.controller.projet.AppelsBilans.racines;
import static com.tsix.immocrm.controller.projet.AppelsBilans.valeur;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.COLONNES;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.COUT_PREVISIONNEL;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.COUT_REEL;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.ESTIMATION_BASE;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.ESTIMATION_CONFIDENTIELLE;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.FAISABILITE;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.MARCHE;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.MARCHE_AJUSTE;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.PREVISIONS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * N5a Bilans — formules par colonne (spec « Bilans — formules sur tables »), arbre R10, Marché racine R11, HT/TTC.
 * Scénario du setUp : projet P, racine R → E1, E2, V ; lot L1 non multi ventilé E1 0,6 / E2 0,4 ; étude de faisabilité
 * sur la tranche T (P taux 3, Q taux 1, commun C taux 5 hors somme), poids 0,8.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BilansProjetSyntheseTest {

    private static final String COMMERCIAL = "Commercial";
    private static final BigDecimal PRECISION = new BigDecimal("0.01");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBilansProjet f;

    private AppelsBilans appels;
    private UUID projet;
    private UUID racine;
    private UUID enfant1;
    private UUID enfant2;
    private UUID vide;

    @BeforeEach
    void setUp() {
        appels = new AppelsBilans(mockMvc);
        f.preparer();
        projet = f.projet("BP", false);
        racine = f.categorie("Bilan racine", null, 2);
        enfant1 = f.categorie("Bilan enfant 1", racine, 1);
        enfant2 = f.categorie("Bilan enfant 2", racine, 2);
        vide = f.categorie("Bilan vide", racine, 3);

        UUID lot = f.lot("L1", projet, false);
        f.ventiler(lot, enfant1, "0.6");
        f.ventiler(lot, enfant2, "0.4");
        f.estimationBase(lot, "1000");
        f.ligneEstimation(lot, "E1", "2", "450");
        f.devis(lot, projet, "DV1", "600", LocalDate.of(2026, 1, 10), true);
        f.devis(lot, projet, "DV2", "960", LocalDate.of(2026, 2, 10), true);
        f.devis(lot, projet, "DV3", "1200", LocalDate.of(2026, 3, 10), false);
        f.ligneDefinitive(lot, "D1", "2", "425");
        f.piece(lot, projet, "FA1", FixtureAchat.FACTURE, "720", LocalDate.of(2026, 4, 1), null);
        f.piece(lot, projet, "AV1", FixtureAchat.AVOIR, "120", LocalDate.of(2026, 4, 2), null);

        UUID tranche = f.tranche("T", projet);
        f.ventilationTranche(projet, tranche, "3");
        f.ventilationTranche(f.projet("BQ", false), tranche, "1");
        f.ventilationTranche(f.projet("BC", true), tranche, "5");
        UUID etude = f.etude("S");
        f.poids(etude, tranche, "0.8");
        f.prevision(etude, enfant1, "1000", "1200", true);
        f.prevision(etude, enfant1, "5000", "6000", false);
    }

    private RequestPostProcessor qui(UUID... projets) {
        return connecte(f.compte("syn." + UUID.randomUUID().toString().substring(0, 6), true, projets), COMMERCIAL);
    }

    private Reponse lire(UUID p, String... autres) throws Exception {
        String[] params = new String[autres.length + 2];
        params[0] = PROJETS;
        params[1] = p.toString();
        System.arraycopy(autres, 0, params, 2, autres.length);
        Reponse r = appels.lire(qui(p), params);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static void attendre(SoftAssertions s, Reponse r, UUID categorie, String colonne, String attendu) {
        s.assertThat(valeur(r, categorie, colonne)).as(colonne).isCloseTo(new BigDecimal(attendu), within(PRECISION));
    }

    // ------------------------------------------------------------------ DTO

    @Test
    void dto_huitColonnesDansLOrdre_etTtcFauxParDefaut() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.ids("$.colonnes[*].code")).containsExactlyElementsOf(COLONNES);
        s.assertThat(r.valeur("$.ttc")).isEqualTo(false);
        s.assertAll();
    }

    // ------------------------------------------------------------------ R9 : formules (HT)

    /** Branche A : 1000 × part (3 / (3 + 1), le projet commun hors somme) × poids 0,8 = 600 ; ligne IsForProject = 0 ignorée. */
    @Test
    void r9_faisabilite_htFoisPartDuProjetDansLaTrancheFoisPoids() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, enfant1, FAISABILITE, "600");
        attendre(s, r, enfant2, FAISABILITE, "0");
        s.assertAll();
    }

    @Test
    void r9_estimationBase_manuelleFoisTauxDeVentilation() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, enfant1, ESTIMATION_BASE, "600");
        attendre(s, r, enfant2, ESTIMATION_BASE, "400");
        s.assertAll();
    }

    /** Σ PrjMarketLineEstimation = 2 × 450 = 900, × 0,6. */
    @Test
    void r9_estimationConfidentielle_sommeDesLignesEstimatives() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, enfant1, ESTIMATION_CONFIDENTIELLE, "540");
        attendre(s, r, enfant2, ESTIMATION_CONFIDENTIELLE, "360");
        s.assertAll();
    }

    /** Devis retenus 500 HT (10/01) et 800 HT (10/02), non retenu 1000 HT (10/03) : le retenu le plus récent, 800 × 0,6. */
    @Test
    void r9_marche_lotNonMulti_devisRetenuLePlusRecent() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, enfant1, MARCHE, "480");
        attendre(s, r, enfant2, MARCHE, "320");
        s.assertAll();
    }

    /** Σ PrjMarketLine = 2 × 425 = 850, × 0,6. */
    @Test
    void r9_marcheAjuste_lotNonMulti_sommeDesLignesDefinitives() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, enfant1, MARCHE_AJUSTE, "510");
        attendre(s, r, enfant2, MARCHE_AJUSTE, "340");
        s.assertAll();
    }

    @Test
    void r9_previsions_marcheAjusteNonNul() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, enfant1, PREVISIONS, "510");
        attendre(s, r, enfant2, PREVISIONS, "340");
        s.assertAll();
    }

    @Test
    void r9_coutPrevisionnel_lotNonTermine_marcheAjuste() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, enfant1, COUT_PREVISIONNEL, "510");
        attendre(s, r, enfant2, COUT_PREVISIONNEL, "340");
        s.assertAll();
    }

    /** Facture 600 HT − avoir 100 HT = 500, × 0,6 ; les devis n'y entrent pas. */
    @Test
    void r9_coutReel_facturesMoinsAvoirs() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, enfant1, COUT_REEL, "300");
        attendre(s, r, enfant2, COUT_REEL, "200");
        s.assertAll();
    }

    /** Lot sans ligne estimative ni définitive ni devis : col. 3 = col. 2, Marché 0, Prévisions = col. 2, Coût prév. = col. 3. */
    @Test
    void r9_lotSansLignes_replisSurEstimationBase_etMarcheZero() throws Exception {
        UUID autre = f.projet("BR", false);
        UUID categorie = f.categorie("Bilan repli", null, 1);
        UUID lot = f.lot("LR", autre, false);
        f.ventiler(lot, categorie, "1");
        f.estimationBase(lot, "700");

        Reponse r = lire(autre);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, categorie, ESTIMATION_CONFIDENTIELLE, "700");
        attendre(s, r, categorie, MARCHE, "0");
        attendre(s, r, categorie, MARCHE_AJUSTE, "0");
        attendre(s, r, categorie, PREVISIONS, "700");
        attendre(s, r, categorie, COUT_PREVISIONNEL, "700");
        s.assertAll();
    }

    /** Lot avec lignes estimatives (3 × 100) mais sans définitives : Prévisions et Coût prévisionnel = col. 3 (300), pas col. 2. */
    @Test
    void r9_lotSansLignesDefinitives_previsionsEtCoutPrevisionnel_estimationConfidentielle() throws Exception {
        UUID autre = f.projet("BE", false);
        UUID categorie = f.categorie("Bilan estimatif", null, 1);
        UUID lot = f.lot("LE", autre, false);
        f.ventiler(lot, categorie, "1");
        f.estimationBase(lot, "900");
        f.ligneEstimation(lot, "X1", "3", "100");

        Reponse r = lire(autre);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, categorie, ESTIMATION_CONFIDENTIELLE, "300");
        attendre(s, r, categorie, PREVISIONS, "300");
        attendre(s, r, categorie, COUT_PREVISIONNEL, "300");
        s.assertAll();
    }

    /** Lot multi : Marché = Σ commandes à statut IsOkForPayement = 1 (1000 HT ; 500 HT non OK ignorée) ; Marché ajusté = Marché. */
    @Test
    void r9_lotMulti_marcheSommeDesCommandesOkPaiement_marcheAjusteEgal() throws Exception {
        UUID autre = f.projet("BM", false);
        UUID categorie = f.categorie("Bilan multi", null, 1);
        UUID lot = f.lot("LM", autre, true);
        f.ventiler(lot, categorie, "1");
        f.commande(lot, autre, "BC1", "1200", true);
        f.commande(lot, autre, "BC2", "600", false);
        f.ligneDefinitive(lot, "M1", "2", "100");

        Reponse r = lire(autre);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, categorie, MARCHE, "1000");
        attendre(s, r, categorie, MARCHE_AJUSTE, "1000");
        s.assertAll();
    }

    /** Lot terminé (statut de type fin succès) : Coût prévisionnel = Σ factures (600 HT), pas le marché ajusté (1000). */
    @Test
    void r9_coutPrevisionnel_lotTermine_sommeDesFactures() throws Exception {
        UUID autre = f.projet("BX", false);
        UUID categorie = f.categorie("Bilan termine", null, 1);
        UUID lot = f.lotTermine("LX", autre, false);
        f.ventiler(lot, categorie, "1");
        f.ligneDefinitive(lot, "X2", "2", "500");
        f.piece(lot, autre, "FAX", FixtureAchat.FACTURE, "720", LocalDate.of(2026, 4, 1), null);

        Reponse r = lire(autre);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, categorie, MARCHE_AJUSTE, "1000");
        attendre(s, r, categorie, COUT_PREVISIONNEL, "600");
        s.assertAll();
    }

    /** Lot annulé : CancelRatio = décompté 400 / bordereau 1000 = 0,4 ; estimation de base 2000 × 0,4 = 800. */
    @Test
    void r9_lotAnnule_montantsFoisCancelRatio() throws Exception {
        UUID autre = f.projet("BN", false);
        UUID categorie = f.categorie("Bilan annule", null, 1);
        UUID lot = f.lotTermine("LN", autre, true);
        f.ventiler(lot, categorie, "1");
        f.estimationBase(lot, "2000");
        f.ligneDefinitive(lot, "N1", "2", "500");
        f.decompte(lot, "DN1", "400");

        Reponse r = lire(autre);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, categorie, ESTIMATION_BASE, "800");
        s.assertAll();
    }

    /** Branche C : lot (2000) d'un projet commun réparti sur le projet non commun de la tranche : 2000 × 1 × VT1 0,5 × VT2 0,75. */
    @Test
    void r9_brancheC_lotDuProjetCommun_reparti_categorieFoisTauxDesDeuxVentilations() throws Exception {
        UUID commun = f.projet("BCC", true);
        UUID cible = f.projet("BCN", false);
        UUID tranche = f.tranche("TC", cible);
        f.ventilationTranche(commun, tranche, "0.5");
        f.ventilationTranche(cible, tranche, "0.75");
        UUID categorie = f.categorie("Bilan commun", null, 1);
        UUID lot = f.lot("LC", commun, false);
        f.ventiler(lot, categorie, "1");
        f.estimationBase(lot, "2000");

        Reponse r = lire(cible);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, categorie, ESTIMATION_BASE, "750");
        s.assertAll();
    }

    // ------------------------------------------------------------------ TTC

    @Test
    void ttc_vrai_valeursTtc_etDrapeauRenvoye() throws Exception {
        Reponse r = lire(projet, TTC, "true");

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.valeur("$.ttc")).isEqualTo(true);
        attendre(s, r, enfant1, FAISABILITE, "720");
        attendre(s, r, enfant1, ESTIMATION_BASE, "720");
        attendre(s, r, enfant1, ESTIMATION_CONFIDENTIELLE, "648");
        attendre(s, r, enfant1, MARCHE, "576");
        attendre(s, r, enfant1, MARCHE_AJUSTE, "612");
        attendre(s, r, enfant1, PREVISIONS, "612");
        attendre(s, r, enfant1, COUT_PREVISIONNEL, "612");
        attendre(s, r, enfant1, COUT_REEL, "360");
        s.assertAll();
    }

    // ------------------------------------------------------------------ R10 / R11 : arbre

    /** R10 parent = cumul des descendants ; R11 Marché de la racine = Σ Marché des enfants (800), pas l'estimation (bug OMRAN). */
    @Test
    void r10_r11_racine_cumulDesEnfants_surLesHuitColonnes() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        attendre(s, r, racine, FAISABILITE, "600");
        attendre(s, r, racine, ESTIMATION_BASE, "1000");
        attendre(s, r, racine, ESTIMATION_CONFIDENTIELLE, "900");
        attendre(s, r, racine, MARCHE, "800");
        attendre(s, r, racine, MARCHE_AJUSTE, "850");
        attendre(s, r, racine, PREVISIONS, "850");
        attendre(s, r, racine, COUT_PREVISIONNEL, "850");
        attendre(s, r, racine, COUT_REEL, "500");
        s.assertAll();
    }

    @Test
    void r10_racineNiveau0_enfantsNiveau1_sousLaRacine() throws Exception {
        Reponse r = lire(projet);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(racines(r, List.of(racine, enfant1, enfant2))).containsExactly(racine.toString());
        s.assertThat(champ(r, racine, "niveau")).isEqualTo(0);
        s.assertThat(champ(r, enfant1, "niveau")).isEqualTo(1);
        s.assertThat(r.ids("$.lignes[?(@.categorieId == '" + racine + "')].enfants[*].categorieId"))
                .containsExactly(enfant1.toString(), enfant2.toString());
        s.assertAll();
    }

    @Test
    void r10_categorieAZeroSurLesHuitColonnes_masquee() throws Exception {
        Reponse r = lire(projet);

        assertThat(occurrences(r, vide)).isZero();
    }

    /** Racines triées par Sorting puis Description : « Zèbre » (1) avant « Alpha » (2), puis « Alpha » / « Beta » à égalité (3). */
    @Test
    void r10_racinesTrieesParSortingPuisLibelle() throws Exception {
        UUID autre = f.projet("BT", false);
        UUID zebre = f.categorie("Zèbre tri", null, 1);
        UUID alpha = f.categorie("Alpha tri", null, 2);
        UUID beta3 = f.categorie("Beta tri", null, 3);
        UUID alpha3 = f.categorie("Alpha tri 3", null, 3);
        UUID lot = f.lot("LT", autre, false);
        f.ventiler(lot, beta3, "0.25");
        f.ventiler(lot, alpha, "0.25");
        f.ventiler(lot, zebre, "0.25");
        f.ventiler(lot, alpha3, "0.25");
        f.estimationBase(lot, "400");

        Reponse r = lire(autre);

        assertThat(racines(r, List.of(zebre, alpha, beta3, alpha3)))
                .containsExactly(zebre.toString(), alpha.toString(), alpha3.toString(), beta3.toString());
    }
}
