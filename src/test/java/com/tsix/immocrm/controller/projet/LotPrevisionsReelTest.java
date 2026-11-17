package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureModelesSimulation;
import com.tsix.immocrm.fixtures.FixtureModelesSimulation.Ligne;
import com.tsix.immocrm.fixtures.FixturePrevisionsReel;
import com.tsix.immocrm.fixtures.FixturePrevisionsReel.TauxStockes;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.FIN_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.FIXEE;
import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.INCHANGEE;
import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.SYSTEME;
import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.detail;
import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.mode;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotFiche.DROIT_ESTIMATION;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.DROIT_PREVISIONS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-lot-previsions-reel.md §3-§4 : « Aligner les prévisions au réel » (A1-A6), « Appliquer un modèle » (M5), « Exporter vers
 * modèle » (M6), droits, ordre 404 → 403 → 400 → 409, aucune écriture. Chiffres faits à la main, en jours depuis le 01/01/2026 (J0) :
 * <ul>
 *   <li>lot 01/01/2026 + 90 j (fin J90 = 01/04) ; bordereau 1000 HT ; points « Prévisions » (J0, 0), (J45 = 15/02, 0,6), (J90, 1),
 *       temps 0 / 0,5 / 1 ;</li>
 *   <li>décompte réel facturé le 15/02 (J45) : HT 200 → avancement réel Ar = 0,2 ; TTC = net = facture = 240.</li>
 * </ul>
 * A3 : point d'avancement &lt; 0,2 = (J0, 0), suivant = (J45, 0,6) → date de référence Dr = J0 + 0,2 × 45 / 0,6 = J15 (16/01).
 * A2 : écart de temps = J45 − J15 = 30 j → {@code ecartJours} −30, {@code finSysteme} = 15/02 + 30 = J75 (17/03) ; prévu au 15/02 = 0,6 →
 * {@code ecartAvancement} = −(0,6 − 0,2) = −0,4 ; facture 240 − net 240 → {@code ecartMontant} 0.
 * A5 : redatage {@code Dfp − (Df − Dprev) × (Dfp − Ddec) / (Df − Dr)}, Df = J90, Dr = J15, Ddec = J45 ; le point J90 reste à Dfp.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPrevisionsReelTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String POINTS_DATE = "$.points[*].date";
    private static final String POINTS_AVANCEMENT = "$.points[*].avancement";
    private static final String MESSAGE_DATE = "Veuillez sélectionnez une date supérieur ou égale à la date dérnière décompte / facture";
    private static final LocalDate J45 = DEBUT_2026.plusDays(45);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;
    @Autowired private FixturePrevisionsReel reel;
    @Autowired private FixtureModelesSimulation modeles;

    private PrevisionsReelSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statut;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        modeles.tables();
        appels = new PrevisionsReelSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        projet = parite.projet("PRR-" + jeton, parite.entiteJuridique("PRE-" + jeton));
        statut = parite.statut(ENTREPRENEURS, "PRS-" + jeton);
        admin = connecte(parite.administrateur("lot.prr." + jeton, projet), "Administrateur");
    }

    /** Lot du projet : planning 01/01 + 90 j, bordereau 1000 HT, points (J0, 0), (J45, 0,6), (J90, 1), sans réel. */
    private UUID lot(UUID projetDuLot, String genre, UUID statutDuLot) {
        rang++;
        UUID lot = parite.lot(projetDuLot, "PRL" + rang + "-" + jeton, genre, statutDuLot);
        prev.planning(lot, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0", "0", "0");
        reel.bordereau(lot, "1000");
        prev.pointPrevision(lot, DEBUT_2026, "0", "0", "0", "0");
        prev.pointPrevision(lot, J45, "0.6", "0.5", "0.6", "720");
        prev.pointPrevision(lot, FIN_2026, "1", "1", "0.4", "480");
        return lot;
    }

    private UUID lot() {
        return lot(projet, ENTREPRENEURS, statut);
    }

    /** Lot de référence : décompte réel facturé le 15/02, avancement 0,2, net 240, facture {@code factureTTC}. */
    private UUID lotAvecReel(String factureTTC) {
        UUID lot = lot();
        reel.decompteFacture(lot, "PRD" + rang + "-" + jeton, 1, J45, "200", "240", factureTTC, TauxStockes.de("0", "0"));
        return lot;
    }

    private RequestPostProcessor avec(String... droits) {
        rang++;
        return connecte(parite.avecDroits("lot.prr" + rang + "." + jeton, List.of(droits), projet), COMMERCIAL);
    }

    private static List<String> dates(Reponse r) {
        return r.ids(POINTS_DATE);
    }

    private static List<BigDecimal> avancements(Reponse r) {
        List<Object> brutes = r.valeur(POINTS_AVANCEMENT) instanceof List<?> l ? List.copyOf(l) : List.of();
        return brutes.stream().map(v -> new BigDecimal(String.valueOf(v))).toList();
    }

    private static Object valeurOuNull(Reponse r, String chemin) {
        try {
            return r.valeur(chemin);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void points(SoftAssertions s, Reponse r, List<LocalDate> dates, String... avancements) {
        s.assertThat(dates(r)).containsExactlyElementsOf(dates.stream().map(LocalDate::toString).toList());
        s.assertThat(avancements(r)).usingElementComparator(BigDecimal::compareTo)
                .containsExactlyElementsOf(Arrays.stream(avancements).map(BigDecimal::new).toList());
    }

    /** Tout ce que les routes « proposition » ne doivent pas toucher : points, planning, prévisions de paiement. */
    private List<Object> etatStocke(UUID lot) {
        return List.of(prev.pointsPrevision(lot), prev.pointsBase(lot), prev.lot(lot), prev.lignesPaiement(lot));
    }

    // ------------------------------------------------------------------ A2 / A3 : valeurs de la modale

    @Test
    void a2_ecarts_lotAvecReel_valeursCalculeesALaMain() throws Exception {
        UUID lot = lotAvecReel("240");

        Reponse r = appels.ecarts(admin, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.finActuelle")).isEqualTo(FIN_2026.toString());
            s.assertThat(r.valeur("$.dateDernierDocumentReel")).isEqualTo(J45.toString());
            s.assertThat(r.valeur("$.finSysteme")).as("15/02 + 30 j").isEqualTo(DEBUT_2026.plusDays(75).toString());
            s.assertThat(r.nombre("$.ecartJours")).as("−(J45 − J15)").isEqualByComparingTo("-30");
            s.assertThat(r.nombre("$.ecartAvancement")).as("−(0,6 − 0,2)").isEqualByComparingTo("-0.4");
            s.assertThat(r.nombre("$.ecartMontant")).isEqualByComparingTo("0");
        });
    }

    @Test
    void a2_ecartMontant_factureMoinsNetDesReels() throws Exception {
        Reponse r = appels.ecarts(admin, lotAvecReel("290"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.nombre("$.ecartMontant")).as("290 − 240").isEqualByComparingTo("50");
    }

    @Test
    void a2_ecartMontant_sousLeSeuilDe0_1_vautZero() throws Exception {
        Reponse r = appels.ecarts(admin, lotAvecReel("240.05"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.nombre("$.ecartMontant")).as("|0,05| < 0,1").isEqualByComparingTo("0");
    }

    // A3 sans réel : Ar = 0, Dr = début, écart nul ; finSysteme = fin + 0.
    @Test
    void a2_ecarts_lotSansReel_tousNuls() throws Exception {
        Reponse r = appels.ecarts(admin, lot());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(valeurOuNull(r, "$.dateDernierDocumentReel")).isNull();
            s.assertThat(r.valeur("$.finSysteme")).isEqualTo(FIN_2026.toString());
            s.assertThat(r.nombre("$.ecartJours")).isEqualByComparingTo("0");
            s.assertThat(r.nombre("$.ecartAvancement")).isEqualByComparingTo("0");
            s.assertThat(r.nombre("$.ecartMontant")).isEqualByComparingTo("0");
        });
    }

    // ------------------------------------------------------------------ A4 / A5 / A6 : proposition

    // INCHANGEE : Dfp = Df = J90 ; J45 → 90 − 45 × 45 / 75 = J63 (05/03) ; J90 inchangé ; réel (J45, 0,2) en tête. Ni fin ni durée.
    @Test
    void a5_inchangee_reelsPuisPrevisionsRedatees_sansFinNiDuree() throws Exception {
        Reponse r = appels.aligner(admin, lotAvecReel("240"), mode(INCHANGEE, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        points(s, r, List.of(J45, DEBUT_2026.plusDays(63), FIN_2026), "0.2", "0.6", "1");
        s.assertThat(valeurOuNull(r, "$.fin")).isNull();
        s.assertThat(valeurOuNull(r, "$.duree")).isNull();
        s.assertAll();
    }

    // SYSTEME : Dfp = J75 ; J45 → 75 − 45 × 30 / 75 = J57 (27/02) ; J90 → J75 (17/03) ; fin 17/03, durée 75.
    @Test
    void a5_systeme_finProposeeParLeSysteme_finEtDuree() throws Exception {
        Reponse r = appels.aligner(admin, lotAvecReel("240"), mode(SYSTEME, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        points(s, r, List.of(J45, DEBUT_2026.plusDays(57), DEBUT_2026.plusDays(75)), "0.2", "0.6", "1");
        s.assertThat(r.valeur("$.fin")).isEqualTo(DEBUT_2026.plusDays(75).toString());
        s.assertThat(r.nombre("$.duree")).isEqualByComparingTo("75");
        s.assertAll();
    }

    // FIXEE 16/04 (J105) : Dfp − Ddec = 60 ; J45 → 105 − 45 × 60 / 75 = J69 (11/03) ; J90 → J105 ; durée 105.
    @Test
    void a5_fixee_dateChoisie_finEtDuree() throws Exception {
        LocalDate fin = DEBUT_2026.plusDays(105);

        Reponse r = appels.aligner(admin, lotAvecReel("240"), mode(FIXEE, fin));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        points(s, r, List.of(J45, DEBUT_2026.plusDays(69), fin), "0.2", "0.6", "1");
        s.assertThat(r.valeur("$.fin")).isEqualTo(fin.toString());
        s.assertThat(r.nombre("$.duree")).isEqualByComparingTo("105");
        s.assertAll();
    }

    // A5.4 : point (J63, 0,8) ajouté ; redatés en mars J63 (0,6) et J73,8 (0,8) → un seul point de mars, le plus avancé.
    @Test
    void a5_parMois_garderLaPrevisionLaPlusAvancee() throws Exception {
        UUID lot = lotAvecReel("240");
        prev.pointPrevision(lot, DEBUT_2026.plusDays(63), "0.8", "0.7", "0.2", "240");

        Reponse r = appels.aligner(admin, lot, mode(INCHANGEE, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(avancements(r)).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("0.2"), new BigDecimal("0.8"), BigDecimal.ONE);
        s.assertThat(dates(r)).extracting(d -> d.substring(0, 7)).containsExactly("2026-02", "2026-03", "2026-04");
        s.assertAll();
    }

    // Sans réel : Dr = Ddec = début → redatage identité ; seuls les points d'avancement > 0 sont repris.
    @Test
    void a5_lotSansReel_previsionsInchangees() throws Exception {
        Reponse r = appels.aligner(admin, lot(), mode(INCHANGEE, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        points(s, r, List.of(J45, FIN_2026), "0.6", "1");
        s.assertAll();
    }

    @Test
    void a4_fixee_avantLeDernierDocumentReel_409_messageOmran() throws Exception {
        Reponse r = appels.aligner(admin, lotAvecReel("240"), mode(FIXEE, J45.minusDays(1)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(detail(r)).contains(MESSAGE_DATE);
    }

    @Test
    void a4_fixee_egaleAuDernierDocumentReel_acceptee() throws Exception {
        Reponse r = appels.aligner(admin, lotAvecReel("240"), mode(FIXEE, J45));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.valeur("$.fin")).isEqualTo(J45.toString());
    }

    @Test
    void a5_lotMultiFournisseur_409_sansProposition() throws Exception {
        UUID multi = lot(projet, FOURNISSEURS, parite.statut(FOURNISSEURS, "PRM-" + jeton));

        Reponse r = appels.aligner(admin, multi, mode(INCHANGEE, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(detail(r)).contains("multi-fournisseur");
    }

    @Test
    void a6_alignement_nEcritRien() throws Exception {
        UUID lot = lotAvecReel("240");
        List<Object> avant = etatStocke(lot);

        Reponse r = appels.aligner(admin, lot, mode(SYSTEME, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(etatStocke(lot)).isEqualTo(avant);
    }

    @Test
    void ordre_modeInconnu_400() throws Exception {
        Reponse r = appels.aligner(admin, lotAvecReel("240"), mode("AUTRE", null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ droits et ordre (§3 Droits)

    @Test
    void droits_alignement_sansDroitPrevisions_403_avecDroit_200() throws Exception {
        UUID lot = lotAvecReel("240");

        Reponse refus = appels.aligner(avec(DROIT_ESTIMATION), lot, mode(INCHANGEE, null));
        Reponse accord = appels.aligner(avec(DROIT_PREVISIONS), lot, mode(INCHANGEE, null));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        s.assertThat(accord.statut()).as(accord.corps()).isEqualTo(200);
        s.assertAll();
    }

    @Test
    void droits_ecarts_avecDroitPrevisions_200() throws Exception {
        Reponse r = appels.ecarts(avec(DROIT_PREVISIONS), lotAvecReel("240"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    @Test
    void ordre_sansDroit_modeInconnu_403AvantLe400() throws Exception {
        Reponse r = appels.aligner(avec(DROIT_ESTIMATION), lotAvecReel("240"), mode("AUTRE", null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void ordre_lotHorsPerimetre_compteSansDroit_404AvantLe403_memeMessageQueLaFiche() throws Exception {
        UUID cache = lot(parite.projet("PRX-" + jeton, parite.entiteJuridique("PRY-" + jeton)), ENTREPRENEURS, statut);
        RequestPostProcessor sansDroit = avec();
        Reponse fiche = appels.fiche(sansDroit, cache);

        Reponse aligner = appels.aligner(sansDroit, cache, mode(INCHANGEE, null));
        Reponse ecarts = appels.ecarts(sansDroit, cache);
        Reponse propose = appels.modelePropose(sansDroit, cache);

        SoftAssertions s = new SoftAssertions();
        for (Reponse r : List.of(aligner, ecarts, propose)) {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
            s.assertThat(detail(r)).isNotBlank().isEqualTo(detail(fiche));
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ M5 : appliquer un modèle

    // Lignes saisies dans le désordre ; date = début + 90 × temps (22,5 j tronqués → 23/01) ; paiement ignoré.
    @Test
    void m5_appliquer_datesDebutPlusDelaiFoisTemps_triParTemps() throws Exception {
        UUID lot = lot();
        UUID modele = modeles.modele("MSA-" + jeton, "Modèle A", null, new Ligne("1", "1", "0.5"),
                new Ligne("0", "0", "0"), new Ligne("0.25", "0.2", "0.2"), new Ligne("0.5", "0.6", "0.3"));

        Reponse r = appels.appliquer(admin, lot, modele);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        points(s, r, List.of(DEBUT_2026, DEBUT_2026.plusDays(22), J45, FIN_2026), "0", "0.2", "0.6", "1");
        s.assertAll();
    }

    @Test
    void m5_appliquer_delaiNul_tousLesPointsAuDebut() throws Exception {
        UUID lot = lot();
        prev.planning(lot, DEBUT_2026, 0, DEBUT_2026, DUREE_90);
        UUID modele = modeles.modele("MSB-" + jeton, "Modèle B", null, new Ligne("0", "0", "0"), new Ligne("1", "1", "1"));

        Reponse r = appels.appliquer(admin, lot, modele);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        points(s, r, List.of(DEBUT_2026, DEBUT_2026), "0", "1");
        s.assertAll();
    }

    @Test
    void m5_appliquer_nEcritRien() throws Exception {
        UUID lot = lot();
        UUID modele = modeles.modele("MSC-" + jeton, "Modèle C", null, new Ligne("0", "0", "0"), new Ligne("1", "1", "1"));
        List<Object> avant = etatStocke(lot);

        Reponse r = appels.appliquer(admin, lot, modele);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(etatStocke(lot)).isEqualTo(avant);
    }

    @Test
    void m5_droits_sansDroitPrevisions_403_avecDroit_200() throws Exception {
        UUID lot = lot();
        UUID modele = modeles.modele("MSD-" + jeton, "Modèle D", null, new Ligne("0", "0", "0"), new Ligne("1", "1", "1"));

        Reponse refus = appels.appliquer(avec(DROIT_ESTIMATION), lot, modele);
        Reponse accord = appels.appliquer(avec(DROIT_PREVISIONS), lot, modele);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        s.assertThat(accord.statut()).as(accord.corps()).isEqualTo(200);
        s.assertAll();
    }

    // Ordre : 404 modèle inconnu avant 403 ; témoin : même compte, modèle connu → 403.
    @Test
    void ordre_modeleInconnu_compteSansDroit_404AvantLe403() throws Exception {
        UUID lot = lot();
        UUID modele = modeles.modele("MSE-" + jeton, "Modèle E", null, new Ligne("0", "0", "0"), new Ligne("1", "1", "1"));
        RequestPostProcessor sansDroit = avec(DROIT_ESTIMATION);

        Reponse inconnu = appels.appliquer(sansDroit, lot, UUID.randomUUID());
        Reponse connu = appels.appliquer(sansDroit, lot, modele);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(inconnu.statut()).as(inconnu.corps()).isEqualTo(404);
        s.assertThat(connu.statut()).as(connu.corps()).isEqualTo(403);
        s.assertAll();
    }

    // ------------------------------------------------------------------ M6 : exporter vers un modèle

    // Points de base insérés dans le désordre : lignes triées par date, temps / avancement / paiement copiés.
    @Test
    void m6_modelePropose_pointsDeBaseCopiesTriesParDate() throws Exception {
        UUID lot = lot();
        prev.pointBase(lot, FIN_2026, "1", "1", "0.5", "600");
        prev.pointBase(lot, DEBUT_2026, "0", "0", "0", "0");
        prev.pointBase(lot, J45, "0.6", "0.5", "0.5", "600");

        Reponse r = appels.modelePropose(admin, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(nombres(r, "$.lignes[*].temps")).containsExactly(0.0, 0.5, 1.0);
        s.assertThat(nombres(r, "$.lignes[*].avancement")).containsExactly(0.0, 0.6, 1.0);
        s.assertThat(nombres(r, "$.lignes[*].paiement")).containsExactly(0.0, 0.5, 0.5);
        s.assertAll();
    }

    @Test
    void m6_modelePropose_sansPointDeBase_lignesVides() throws Exception {
        Reponse r = appels.modelePropose(admin, lot());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(nombres(r, "$.lignes[*].temps")).isEmpty();
    }

    @Test
    void m6_modelePropose_nEcritRien_niModele() throws Exception {
        UUID lot = lot();
        prev.pointBase(lot, DEBUT_2026, "0", "0", "0", "0");
        prev.pointBase(lot, FIN_2026, "1", "1", "1", "1200");
        List<Object> avant = etatStocke(lot);
        int modelesAvant = parite.compter("SELECT COUNT(*) FROM PrjMarketPrevisionModel");

        Reponse r = appels.modelePropose(admin, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(etatStocke(lot)).isEqualTo(avant);
        s.assertThat(parite.compter("SELECT COUNT(*) FROM PrjMarketPrevisionModel")).isEqualTo(modelesAvant);
        s.assertAll();
    }

    @Test
    void m6_droits_sansDroitPrevisions_403_avecDroit_200() throws Exception {
        UUID lot = lot();

        Reponse refus = appels.modelePropose(avec(DROIT_ESTIMATION), lot);
        Reponse accord = appels.modelePropose(avec(DROIT_PREVISIONS), lot);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        s.assertThat(accord.statut()).as(accord.corps()).isEqualTo(200);
        s.assertAll();
    }

    private static List<Double> nombres(Reponse r, String chemin) {
        Object v = r.valeur(chemin);
        List<?> l = v instanceof List<?> liste ? liste : List.of();
        return l.stream().map(x -> Double.valueOf(String.valueOf(x))).toList();
    }
}
