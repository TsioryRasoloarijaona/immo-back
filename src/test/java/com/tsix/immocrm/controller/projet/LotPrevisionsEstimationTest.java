package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
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
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.MOIS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Série « Estimation de base » du graphe (FEATURE-lot-previsions.md §3.3, E1-E9, R9), calculée à la lecture par
 * {@code GET /lots/{id}/previsions}. Lot de fixture : base 01/01/2026 + 90 j (fin 01/04/2026), B = 1200 TTC (estimation de
 * base, aucun bordereau ni marché), délai de paiement 0. Chiffres faits à la main dans chaque test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPrevisionsEstimationTest {

    private static final String DECEMBRE = "2025-12-01";
    private static final String JANVIER = "2026-01-01";
    private static final String FEVRIER = "2026-02-01";
    private static final String MARS = "2026-03-01";
    private static final String AVRIL = "2026-04-01";
    private static final String MAI = "2026-05-01";
    private static final BigDecimal CENTIME = new BigDecimal("0.01");
    private static final BigDecimal MONTANT_B = new BigDecimal("1200");
    private static final double PRECISION = 0.0001;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;
    @Autowired private FixtureLotSousFenetres sousFenetres;
    @Autowired private FixtureDecomptes decomptes;

    private PrevisionsLotSupport appels;
    private String jeton;
    private UUID projet;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        appels = new PrevisionsLotSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        projet = parite.projet("PVG-" + jeton, parite.entiteJuridique("PVF-" + jeton));
        admin = connecte(parite.administrateur("lot.pvg." + jeton, projet), "Administrateur");
    }

    /** Lot à base 01/01/2026 + 90 j, B = 1200, avance 10 %, RG 10 %, garantie 1 mois. */
    private UUID lot(String genre) {
        rang++;
        UUID lot = parite.lot(projet, "PVG" + rang + "-" + jeton, genre, parite.statut(genre, "PVS" + rang + "-" + jeton));
        prev.planning(lot, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0.1", "0.1", "1");
        return lot;
    }

    private Reponse graphe(UUID lot) throws Exception {
        Reponse r = appels.graphe(admin, lot);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static BigDecimal estimation(Reponse r, int mois) {
        return r.nombre("$.mois[" + mois + "].estimation");
    }

    private static BigDecimal totalEstimation(Reponse r) {
        List<?> valeurs = (List<?>) r.valeur("$.mois[*].estimation");
        return valeurs.stream().filter(Objects::nonNull).map(v -> new BigDecimal(String.valueOf(v)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void montants(SoftAssertions s, Reponse r, String... attendus) {
        for (int i = 0; i < attendus.length; i++) {
            s.assertThat(estimation(r, i)).as("mois %d", i).isCloseTo(new BigDecimal(attendus[i]), within(CENTIME));
        }
    }

    // ------------------------------------------------------------------ E1-E3, E6, E7, E9 : courbe générée, lot non multi

    /**
     * Courbe générée E1 : n = 3, points 31/01 (1/3), 28/02 (2/3), 31/03 (1). E2 avance 120 au 01/01. E3 chaque décompte
     * 400 − avance 40 − RG 40 = 320. E6 RG 120 au 01/05 (fin + 1 mois). E9 : décembre (premier mois − 1) à mai.
     */
    @Test
    void e1_e2_e3_e6_e7_e9_courbeGeneree_avanceDecomptesNetsEtRg() throws Exception {
        Reponse r = graphe(lot(ENTREPRENEURS));

        assertThat(r.ids(MOIS)).containsExactly(DECEMBRE, JANVIER, FEVRIER, MARS, AVRIL, MAI);
        SoftAssertions.assertSoftly(s -> {
            montants(s, r, "0", "440", "320", "320", "0", "120");
            s.assertThat(totalEstimation(r)).as("Σ mois = Σ lignes E2-E6 = B").isCloseTo(MONTANT_B, within(CENTIME));
        });
    }

    /** E8 : 0 ajouté au 01/01 puis 1/3 au 31/01 — dernière valeur du mois par date ; 2/3 en février ; 1 en mars. */
    @Test
    void e8_avancementEstimation_deZeroAUn_derniereValeurDuMois() throws Exception {
        Reponse r = graphe(lot(ENTREPRENEURS));

        assertThat(r.ids(MOIS)).contains(JANVIER, FEVRIER, MARS);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.mois[1].avancementEstimation").doubleValue()).isCloseTo(1.0 / 3, within(PRECISION));
            s.assertThat(r.nombre("$.mois[2].avancementEstimation").doubleValue()).isCloseTo(2.0 / 3, within(PRECISION));
            s.assertThat(r.nombre("$.mois[3].avancementEstimation")).isEqualByComparingTo("1");
        });
    }

    // ------------------------------------------------------------------ E3 multi-fournisseurs

    /** Multi : « Facture prévisionnel » de B × incrément = 400 par mois, ni avance (E2) ni RG (E6). */
    @Test
    void e3_multiFournisseurs_facturesSansRetenue() throws Exception {
        Reponse r = graphe(lot(FOURNISSEURS));

        assertThat(r.ids(MOIS)).containsExactly(DECEMBRE, JANVIER, FEVRIER, MARS);
        SoftAssertions.assertSoftly(s -> montants(s, r, "0", "400", "400", "400"));
    }

    // ------------------------------------------------------------------ §3.1 PrevisionTTC : marché ajusté d'abord

    /**
     * §3.1 {@code PrevisionTTC} = premier non nul de marché ajusté, confidentielle, base : bordereau 1500 HT + 20 % = 1800 TTC
     * l'emporte sur l'estimation de base 1200. Sans retenue : 1800 / 3 = 600 par mois (1200 / 3 = 400 si la base était lue).
     */
    @Test
    void e3_prixDesPrevisions_marcheAjustePrioritaireSurEstimationDeBase() throws Exception {
        UUID lot = lot(ENTREPRENEURS);
        prev.retenues(lot, "0", "0", "0");
        decomptes.ligneMarche(lot, 1, "1", "1500", "0.2");

        Reponse r = graphe(lot);

        assertThat(r.ids(MOIS)).containsExactly(DECEMBRE, JANVIER, FEVRIER, MARS);
        SoftAssertions.assertSoftly(s -> montants(s, r, "0", "600", "600", "600"));
    }

    // ------------------------------------------------------------------ E1 points saisis, E3 un point par mois

    /** Points saisis 10/01 (0,2), 20/01 (0,5), 15/03 (1) : janvier garde le maximum 0,5 → 600 ; mars 0,5 → 600. */
    @Test
    void e1_e3_pointsSaisis_unSeulParMois_leMaximum() throws Exception {
        UUID lot = lot(FOURNISSEURS);
        prev.pointBase(lot, LocalDate.of(2026, 1, 10), "0.2", "0.1", "0.2", "240");
        prev.pointBase(lot, LocalDate.of(2026, 1, 20), "0.5", "0.2111111111111111", "0.3", "360");
        prev.pointBase(lot, LocalDate.of(2026, 3, 15), "1", "0.8111111111111111", "0.5", "600");

        Reponse r = graphe(lot);

        assertThat(r.ids(MOIS)).containsExactly(DECEMBRE, JANVIER, FEVRIER, MARS);
        SoftAssertions.assertSoftly(s -> montants(s, r, "0", "600", "0", "600"));
    }

    // ------------------------------------------------------------------ E4 solde

    /** Dernier avancement 0,6 < 1 : solde B × 0,4 = 480 daté de la fin de base (01/04). */
    @Test
    void e4_dernierAvancementInferieurA1_soldeALaFinDeBase() throws Exception {
        UUID lot = lot(FOURNISSEURS);
        prev.pointBase(lot, LocalDate.of(2026, 1, 15), "0.3", "0.1555555555555556", "0.3", "360");
        prev.pointBase(lot, LocalDate.of(2026, 2, 15), "0.6", "0.5", "0.3", "360");

        Reponse r = graphe(lot);

        assertThat(r.ids(MOIS)).containsExactly(DECEMBRE, JANVIER, FEVRIER, MARS, AVRIL);
        SoftAssertions.assertSoftly(s -> {
            montants(s, r, "0", "360", "360", "0", "480");
            s.assertThat(totalEstimation(r)).isCloseTo(MONTANT_B, within(CENTIME));
        });
    }

    // ------------------------------------------------------------------ E6 échéances de RG

    /** Deux échéances de 50 % au 15/06 et au 15/07 : RG 120 répartie 60 / 60, plus rien à fin + 1 mois (mai). */
    @Test
    void e6_echeancesRg_repartiesASesDates() throws Exception {
        UUID lot = lot(ENTREPRENEURS);
        sousFenetres.echeanceRg(lot, 1, "RG 1", 0, LocalDate.of(2026, 6, 15), "0.5");
        sousFenetres.echeanceRg(lot, 2, "RG 2", 0, LocalDate.of(2026, 7, 15), "0.5");

        Reponse r = graphe(lot);

        assertThat(r.ids(MOIS)).containsExactly(DECEMBRE, JANVIER, FEVRIER, MARS, AVRIL, MAI, "2026-06-01", "2026-07-01");
        SoftAssertions.assertSoftly(s -> {
            montants(s, r, "0", "440", "320", "320", "0", "0", "60", "60");
            s.assertThat(totalEstimation(r)).isCloseTo(MONTANT_B, within(CENTIME));
        });
    }

    // ------------------------------------------------------------------ R9 : lecture sans écriture ; cas limite

    @Test
    void r9_lecture_nEcritRien() throws Exception {
        UUID lot = lot(ENTREPRENEURS);

        graphe(lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(prev.lignesPaiement(lot)).isEmpty();
            s.assertThat(prev.pointsBase(lot)).isEmpty();
            s.assertThat(prev.pointsPrevision(lot)).isEmpty();
        });
    }

    @Test
    void temoin_datesEgalesEtBaseNulle_200() throws Exception {
        UUID lot = lot(ENTREPRENEURS);
        prev.planning(lot, DEBUT_2026, 0, DEBUT_2026, 0);
        prev.estimationBase(lot, "0", "0", "0", "0.2");

        Reponse r = appels.graphe(admin, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }
}
