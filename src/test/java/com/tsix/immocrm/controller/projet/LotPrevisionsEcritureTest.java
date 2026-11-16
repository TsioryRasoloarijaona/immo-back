package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_BASE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.FIN_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.MODIFIABLE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.POINTS;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.POINTS_BASE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.inchange;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.nombre;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.point;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.saisie;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.texte;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * FEATURE-lot-previsions.md §4.2 / §4.3, R3-R7 (compte Administrateur : tous les droits). Lot de fixture : 01/01/2026 + 90 j
 * sur les deux onglets, estimation de base 1000 / 200 / 1200 (TVA par défaut 20 %), bordereau estimatif 10 × 100 HT
 * (TTC 1200) et {@code EstimatedPriceTTC} = 1200 : le prix des prévisions vaut 1200 quelle que soit sa source.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPrevisionsEcritureTest {

    private static final String FIN_AVANT_DEBUT = "La date fin des prévisions doit être supérieure ou égale à la date début !";
    private static final String DEUX_LIGNES = "Vous devez saisir au moins deux lignes de prévisions avec obligatoirement"
            + " un pourcentage d'avancement allant de 0% et 100%";
    private static final String APRES_FIN = "La date fin des prévisions doit inférieure ou égale à la date fin";
    private static final String AVANT_DEBUT = "La date début des prévisions doit être supérieure ou égale à la date fin";
    private static final String DECROISSANT = "Veuillez vérifier l'avancement dans la prevision du date 15/02/2026 !";
    private static final LocalDate MI_FEVRIER = LocalDate.of(2026, 2, 15);
    private static final LocalDate FIN_JANVIER = LocalDate.of(2026, 1, 31);
    private static final String AVANCEMENT = "PointAchivement";
    private static final String TEMPS = "PointTime";
    private static final String PAIEMENT = "PointPayment";
    private static final String MONTANT = "PointAmount";
    private static final String DATE = "date";
    private static final String HT = "EstimatedPriceHTManual";
    private static final String TVA = "EstimatedVatAmountManual";
    private static final String TTC = "EstimatedPriceTTCManual";
    private static final double PRECISION = 0.000001;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureLotPrevisions prev;

    private PrevisionsLotSupport appels;
    private String jeton;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new PrevisionsLotSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        UUID projet = parite.projet("PVP-" + jeton, parite.entiteJuridique("PVE-" + jeton));
        lot = parite.lot(projet, "PVL-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "PVT-" + jeton));
        prev.planning(lot, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0", "0", "0");
        prev.prixEstime(lot, "1200");
        fiche.ligneEstimative(lot, "PVA-" + jeton, "10", "100");
        admin = connecte(parite.administrateur("lot.pve." + jeton, projet), "Administrateur");
    }

    private Reponse enregistrer(CorpsJson corps) throws Exception {
        return appels.enregistrer(admin, lot, corps);
    }

    private static CorpsJson avecPoints(List<CorpsJson> points) {
        return inchange().avec(POINTS, points);
    }

    private static CorpsJson avecPointsBase(List<CorpsJson> points) {
        return inchange().avec(POINTS_BASE, points);
    }

    private static void refus(Reponse r, String... extraits) {
        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).isNotNull().contains(extraits);
    }

    // ------------------------------------------------------------------ témoins 200

    @Test
    void temoin_put_200_renvoieLeGrapheDuLot() throws Exception {
        Reponse r = enregistrer(inchange());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.valeur("$.lotId")).isEqualTo(lot.toString());
        assertThat(r.valeur(MODIFIABLE)).isEqualTo(true);
    }

    @Test
    void saisie_200_champsDuLot_listesVidesSansPointStocke() throws Exception {
        Reponse r = appels.saisie(admin, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.lotId")).isEqualTo(lot.toString());
            s.assertThat(r.valeur("$.description")).isEqualTo("Lot PVL-" + jeton);
            s.assertThat(r.valeur("$.modifiable")).isEqualTo(true);
            s.assertThat(r.valeur("$.estimationDeBaseModifiable")).isEqualTo(true);
            s.assertThat(r.valeur("$.debut")).isEqualTo(DEBUT_2026.toString());
            s.assertThat(r.nombre("$.duree")).isEqualByComparingTo("90");
            s.assertThat(r.valeur("$.fin")).isEqualTo(FIN_2026.toString());
            s.assertThat(r.valeur("$.debutBase")).isEqualTo(DEBUT_2026.toString());
            s.assertThat(r.nombre("$.dureeBase")).isEqualByComparingTo("90");
            s.assertThat(r.valeur("$.finBase")).isEqualTo(FIN_2026.toString());
            s.assertThat(r.nombre("$.estimationBaseHT")).isEqualByComparingTo("1000");
            s.assertThat(r.nombre("$.estimationBaseTVA")).isEqualByComparingTo("200");
            s.assertThat(r.nombre("$.estimationBaseTTC")).isEqualByComparingTo("1200");
            s.assertThat(r.nombre("$.tvaParDefaut")).as("DefaultVat × 100").isEqualByComparingTo("20");
            s.assertThat(r.nombre("$.prixPrevisions")).isEqualByComparingTo("1200");
            s.assertThat(r.ids("$.points[*].id")).isEmpty();
            s.assertThat(r.ids("$.pointsBase[*].id")).isEmpty();
        });
    }

    @Test
    void saisie_pointsStockes_renvoyesTriesParDate() throws Exception {
        UUID dernier = prev.pointPrevision(lot, FIN_2026, "1", "1", "0.6", "720");
        UUID premier = prev.pointPrevision(lot, MI_FEVRIER, "0.4", "0.5", "0.4", "480");
        UUID base = prev.pointBase(lot, FIN_JANVIER, "0.5", "0.333333", "0.5", "600");

        Reponse r = appels.saisie(admin, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$.points[*].id")).containsExactly(premier.toString(), dernier.toString());
            s.assertThat(r.valeur("$.points[0].date")).isEqualTo(MI_FEVRIER.toString());
            s.assertThat(r.nombre("$.points[0].avancement")).isEqualByComparingTo("0.4");
            s.assertThat(r.nombre("$.points[0].temps")).isEqualByComparingTo("0.5");
            s.assertThat(r.nombre("$.points[0].paiement")).isEqualByComparingTo("0.4");
            s.assertThat(r.nombre("$.points[0].montant")).isEqualByComparingTo("480");
            s.assertThat(r.ids("$.pointsBase[*].id")).containsExactly(base.toString());
            s.assertThat(r.nombre("$.pointsBase[0].montant")).isEqualByComparingTo("600");
        });
    }

    // ------------------------------------------------------------------ R3 : dates

    @Test
    void r3_dureeNegative_400_messageOmran_rienEcrit() throws Exception {
        Reponse r = enregistrer(inchange().avec(DEBUT, LocalDate.of(2026, 2, 1)).avec(DUREE, -1));

        refus(r, FIN_AVANT_DEBUT);
        assertThat(String.valueOf(prev.lot(lot).get("StartDate"))).isEqualTo(DEBUT_2026.toString());
    }

    @Test
    void r3_dureeBaseNegative_400_varianteDeBase() throws Exception {
        refus(enregistrer(inchange().avec(DUREE_BASE, -1)), "La date fin des prévisions de base doit être supérieure ou égale"
                + " à la date début");
    }

    @Test
    void r3_debutAbsent_400() throws Exception {
        Reponse r = enregistrer(inchange().avec(DEBUT, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void r3_dureeBaseAbsente_400() throws Exception {
        Reponse r = enregistrer(inchange().avec(DUREE_BASE, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ R4 : points

    @Test
    void r4_unSeulPoint_400_messageOmran() throws Exception {
        refus(enregistrer(avecPoints(List.of(point(FIN_2026, "1")))), DEUX_LIGNES);
    }

    @Test
    void r4_unSeulPointDeBase_400() throws Exception {
        refus(enregistrer(avecPointsBase(List.of(point(FIN_2026, "1")))), "Vous devez saisir au moins deux lignes de prévisions");
    }

    @Test
    void r4_pointApresLaFin_400_messageOmran() throws Exception {
        refus(enregistrer(avecPoints(List.of(point(DEBUT_2026, "0"), point(FIN_2026.plusDays(1), "1")))), APRES_FIN);
    }

    @Test
    void r4_pointDeBaseApresLaFin_400() throws Exception {
        refus(enregistrer(avecPointsBase(List.of(point(DEBUT_2026, "0"), point(FIN_2026.plusDays(1), "1")))),
                "doit inférieure ou égale à la date fin");
    }

    @Test
    void r4_pointAvantLeDebut_400_messageOmran() throws Exception {
        refus(enregistrer(avecPoints(List.of(point(DEBUT_2026.minusDays(1), "0"), point(FIN_2026, "1")))), AVANT_DEBUT);
    }

    @Test
    void r4_pointDeBaseAvantLeDebut_400() throws Exception {
        refus(enregistrer(avecPointsBase(List.of(point(DEBUT_2026.minusDays(1), "0"), point(FIN_2026, "1")))),
                "doit être supérieure ou égale à la date fin");
    }

    @Test
    void r4_avancementDecroissant_400_messageAvecLaDate() throws Exception {
        refus(enregistrer(avecPoints(List.of(point(FIN_JANVIER, "0.6"), point(MI_FEVRIER, "0.4")))), DECROISSANT);
    }

    @Test
    void r4_avancementDeBaseDecroissant_400_messageAvecLaDate() throws Exception {
        refus(enregistrer(avecPointsBase(List.of(point(FIN_JANVIER, "0.6"), point(MI_FEVRIER, "0.4")))),
                "Veuillez vérifier l'avancement dans la prevision", "15/02/2026");
    }

    @Test
    void r4_avancementSuperieurA1_400() throws Exception {
        Reponse r = enregistrer(avecPoints(List.of(point(DEBUT_2026, "0"), point(FIN_2026, "1.5"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void r4_avancementNegatif_400() throws Exception {
        Reponse r = enregistrer(avecPointsBase(List.of(point(DEBUT_2026, "-0.1"), point(FIN_2026, "1"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ R5 : remplacement

    @Test
    void r5_deuxEnvois_seulesLesLignesDuSecond() throws Exception {
        enregistrer(inchange().avec(POINTS, List.of(point(DEBUT_2026, "0"), point(MI_FEVRIER, "0.5"), point(FIN_2026, "1")))
                .avec(POINTS_BASE, List.of(point(DEBUT_2026, "0"), point(FIN_2026, "1"))));

        Reponse r = enregistrer(inchange().avec(POINTS, List.of(point(DEBUT_2026, "0"), point(LocalDate.of(2026, 3, 1), "1")))
                .avec(POINTS_BASE, List.of(point(DEBUT_2026, "0"), point(FIN_JANVIER, "0.5"), point(FIN_2026, "1"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(prev.pointsPrevision(lot)).extracting(l -> texte(l, DATE))
                    .containsExactly(DEBUT_2026.toString(), "2026-03-01");
            s.assertThat(prev.pointsBase(lot)).extracting(l -> texte(l, DATE))
                    .containsExactly(DEBUT_2026.toString(), FIN_JANVIER.toString(), FIN_2026.toString());
        });
    }

    @Test
    void r5_listesVides_plusAucunPointStocke() throws Exception {
        prev.pointPrevision(lot, DEBUT_2026, "0", "0", "0", "0");
        prev.pointPrevision(lot, FIN_2026, "1", "1", "1", "1200");
        prev.pointBase(lot, DEBUT_2026, "0", "0", "0", "0");
        prev.pointBase(lot, FIN_2026, "1", "1", "1", "1200");

        Reponse r = enregistrer(inchange());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(prev.pointsPrevision(lot)).isEmpty();
            s.assertThat(prev.pointsBase(lot)).isEmpty();
        });
    }

    // ------------------------------------------------------------------ R6 : valeurs stockées

    @Test
    void r6_pointsPrevisions_tempsPaiementMontant_envoiNonTrie() throws Exception {
        Reponse r = enregistrer(avecPoints(List.of(point(FIN_2026, "1"), point(DEBUT_2026, "0"), point(MI_FEVRIER, "0.4"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> points = prev.pointsPrevision(lot);
        assertThat(points).hasSize(3);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(points).extracting(l -> texte(l, DATE))
                    .containsExactly(DEBUT_2026.toString(), MI_FEVRIER.toString(), FIN_2026.toString());
            s.assertThat(points).extracting(l -> nombre(l, TEMPS).doubleValue()).as("(date − début) ÷ durée")
                    .containsExactly(0.0, 0.5, 1.0);
            s.assertThat(points).extracting(l -> nombre(l, PAIEMENT).doubleValue()).as("avancement − précédent")
                    .containsExactly(0.0, 0.4, 0.6);
            s.assertThat(points).extracting(l -> nombre(l, MONTANT).doubleValue()).as("paiement × 1200")
                    .containsExactly(0.0, 480.0, 720.0);
            s.assertThat(points).extracting(l -> nombre(l, AVANCEMENT).doubleValue()).containsExactly(0.0, 0.4, 1.0);
        });
    }

    @Test
    void r6_pointsDeBase_montantSurEstimationDeBaseTtc() throws Exception {
        Reponse r = enregistrer(avecPointsBase(List.of(point(DEBUT_2026, "0"), point(FIN_JANVIER, "0.5"), point(FIN_2026, "1"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> points = prev.pointsBase(lot);
        assertThat(points).hasSize(3);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(points.get(1), TEMPS).doubleValue()).as("30 ÷ 90").isCloseTo(1.0 / 3, within(PRECISION));
            s.assertThat(nombre(points.get(2), TEMPS)).isEqualByComparingTo("1");
            s.assertThat(points).extracting(l -> nombre(l, PAIEMENT).doubleValue()).containsExactly(0.0, 0.5, 0.5);
            s.assertThat(points).extracting(l -> nombre(l, MONTANT).doubleValue()).as("paiement × EstimatedPriceTTCManual")
                    .containsExactly(0.0, 600.0, 600.0);
        });
    }

    // ------------------------------------------------------------------ R7 : estimation de base et dates

    @Test
    void r7_tvaEgaleHtFoisTvaParDefaut_finsCalculees_rienDAutre() throws Exception {
        prev.retenues(lot, "0.05", "0.1", "0");
        CorpsJson corps = saisie(LocalDate.of(2026, 2, 1), 59, LocalDate.of(2026, 1, 15), 30, "2000", List.of(), List.of());

        Reponse r = enregistrer(corps);

        Map<String, Object> enBase = prev.lot(lot);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(enBase, HT)).isEqualByComparingTo("2000");
            s.assertThat(nombre(enBase, TVA)).as("2000 × 0,2").isEqualByComparingTo("400");
            s.assertThat(nombre(enBase, TTC)).isEqualByComparingTo("2400");
            s.assertThat(texte(enBase, "StartDate")).isEqualTo("2026-02-01");
            s.assertThat(enBase).containsEntry("Delay", 59).containsEntry("DelayManual", 30);
            s.assertThat(texte(enBase, "EndDate")).as("début + 59 j").isEqualTo("2026-04-01");
            s.assertThat(texte(enBase, "StartDateManual")).isEqualTo("2026-01-15");
            s.assertThat(texte(enBase, "EndDateManual")).as("début de base + 30 j").isEqualTo("2026-02-14");
            s.assertThat(nombre(enBase, "WarrantyRatio")).as("non écrit").isEqualByComparingTo("0.1");
            s.assertThat(nombre(enBase, "AdvanceRatio")).as("non écrit").isEqualByComparingTo("0.05");
            s.assertThat(texte(enBase, "Description")).as("non écrit").isEqualTo("Lot PVL-" + jeton);
        });
    }

    @Test
    void r7_tvaParDefautNulle_tvaZero_ttcEgalHt() throws Exception {
        prev.estimationBase(lot, "1000", "200", "1200", null);

        Reponse r = enregistrer(inchange().avec(PrevisionsLotSupport.ESTIMATION_BASE_HT, new BigDecimal("1500")));

        Map<String, Object> enBase = prev.lot(lot);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(enBase, HT)).isEqualByComparingTo("1500");
            s.assertThat(nombre(enBase, TVA)).isEqualByComparingTo("0");
            s.assertThat(nombre(enBase, TTC)).isEqualByComparingTo("1500");
        });
    }
}
