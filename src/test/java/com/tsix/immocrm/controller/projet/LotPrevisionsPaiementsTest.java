package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
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

import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.FIN_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.POINTS;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.inchange;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.nombre;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.point;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.somme;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.texte;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.ENTITE_AVANCE;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.ENTITE_DECOMPTE;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.ENTITE_FACTURE_MULTI;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.ENTITE_RG;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Prévisions de paiement {@code PrjMarketPaymentPrevision} (FEATURE-lot-previsions.md §3.4, P1-P12, R8), réécrites par
 * le {@code PUT /lots/{id}/previsions} puis relues en base. Lot de fixture : prévisions 01/01/2026 + 90 j (fin 01/04/2026),
 * aucun bordereau ni marché → {@code Amount} = estimation confidentielle = estimation de base 1000 HT / 1200 TTC,
 * aucun décompte réel, délai de paiement 0. Chiffres faits à la main dans chaque test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPrevisionsPaiementsTest {

    private static final String DESCRIPTION = "Description";
    private static final String MONTANT = "Amount";
    private static final String HT = "AmountHT";
    private static final String TTC = "AmountTTC";
    private static final String AVANCEMENT = "Achievement";
    private static final String DATE = "date";
    private static final String ENTITE = "entite";
    private static final String DECOMPTE_N = "Décompte prévisionnel N° ";
    private static final String RG = "Remboursement de la retenue de garantie prévisionnelle";
    private static final BigDecimal CENTIME = new BigDecimal("0.01");
    private static final BigDecimal TIERS_TTC = new BigDecimal("400");
    private static final BigDecimal TIERS_HT = new BigDecimal("333.33");
    private static final double PRECISION = 0.00001;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;
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
        projet = parite.projet("PVQ-" + jeton, parite.entiteJuridique("PVR-" + jeton));
        admin = connecte(parite.administrateur("lot.pvq." + jeton, projet), "Administrateur");
    }

    private UUID lot(String genre, String avance, String rg, String moisGarantie) {
        rang++;
        UUID lot = parite.lot(projet, "PVQ" + rang + "-" + jeton, genre, parite.statut(genre, "PVU" + rang + "-" + jeton));
        prev.planning(lot, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, avance, rg, moisGarantie);
        return lot;
    }

    /** PUT sans changement (déclencheur R8) puis lecture des lignes du lot, triées par date. */
    private List<Map<String, Object>> recalculer(UUID lot, CorpsJson corps) throws Exception {
        Reponse r = appels.enregistrer(admin, lot, corps);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return prev.lignesPaiement(lot);
    }

    private static List<Map<String, Object>> avecLibelle(List<Map<String, Object>> lignes, String debut) {
        return lignes.stream().filter(l -> texte(l, DESCRIPTION).startsWith(debut)).toList();
    }

    private static void entite(SoftAssertions s, List<Map<String, Object>> lignes, UUID attendue) {
        s.assertThat(lignes).isNotEmpty().allSatisfy(l -> assertThat(texte(l, ENTITE)).isEqualToIgnoringCase(attendue.toString()));
    }

    // ------------------------------------------------------------------ P1, P6, P12 : courbe générée

    /**
     * P1 sans point : n = 3 (31/01, 28/02, 31/03 ; 1/3, 2/3, 1). P6 sans avance ni RG : 400 par décompte, HT = 1000 × 1/3,
     * TTC = 1200 × 1/3, {@code Achievement} = avancement du point. P12 : entité {@code PrjInvoiceDebitWorkingOut}.
     */
    @Test
    void p1_p6_p12_courbeGeneree_troisDecomptesPrevisionnels() throws Exception {
        List<Map<String, Object>> lignes = recalculer(lot(ENTREPRENEURS, "0", "0", "0"), inchange());

        assertThat(lignes).extracting(l -> texte(l, DESCRIPTION))
                .containsExactly(DECOMPTE_N + 1, DECOMPTE_N + 2, DECOMPTE_N + 3);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes).extracting(l -> texte(l, DATE).substring(0, 7)).as("mois de chaque point redaté")
                    .containsExactly("2026-01", "2026-02", "2026-03");
            for (Map<String, Object> l : lignes) {
                s.assertThat(nombre(l, MONTANT)).isCloseTo(TIERS_TTC, within(CENTIME));
                s.assertThat(nombre(l, TTC)).isCloseTo(TIERS_TTC, within(CENTIME));
                s.assertThat(nombre(l, HT)).isCloseTo(TIERS_HT, within(CENTIME));
            }
            double[] avancements = {1.0 / 3, 2.0 / 3, 1.0};
            for (int i = 0; i < lignes.size(); i++) {
                s.assertThat(nombre(lignes.get(i), AVANCEMENT).doubleValue()).isCloseTo(avancements[i], within(PRECISION));
            }
            entite(s, lignes, ENTITE_DECOMPTE);
        });
    }

    // ------------------------------------------------------------------ §3.1 Amount : marché ajusté avant l'estimation

    /**
     * §3.1 {@code Amount} = marché ajusté TTC s'il est non nul : ligne de bordereau 1500 HT, TVA 20 % → 1800 TTC, l'estimation
     * de base 1200 est ignorée. P6 : 600 par décompte, {@code AmountTTC} = {@code CostTTC} (marché) × 1/3 = 600, HT 500.
     */
    @Test
    void p6_prixDesPrevisions_marcheAjustePrioritaireSurEstimation() throws Exception {
        UUID lot = lot(ENTREPRENEURS, "0", "0", "0");
        decomptes.ligneMarche(lot, 1, "1", "1500", "0.2");

        List<Map<String, Object>> lignes = recalculer(lot, inchange());

        assertThat(avecLibelle(lignes, DECOMPTE_N)).hasSize(3);
        SoftAssertions.assertSoftly(s -> {
            for (Map<String, Object> l : avecLibelle(lignes, DECOMPTE_N)) {
                s.assertThat(nombre(l, MONTANT)).isCloseTo(new BigDecimal("600"), within(CENTIME));
                s.assertThat(nombre(l, TTC)).isCloseTo(new BigDecimal("600"), within(CENTIME));
                s.assertThat(nombre(l, HT)).isCloseTo(new BigDecimal("500"), within(CENTIME));
            }
        });
    }

    // ------------------------------------------------------------------ P6 : points saisis

    /** Points 01/01 (0), 15/02 (0,4), 01/04 (1) : le premier est ignoré (≤ 0 et date ≤ début) ; 480 puis 720. */
    @Test
    void p6_pointsSaisis_numerotesDatesEtMontants() throws Exception {
        UUID lot = lot(ENTREPRENEURS, "0", "0", "0");

        List<Map<String, Object>> lignes = recalculer(lot, inchange().avec(POINTS,
                List.of(point(DEBUT_2026, "0"), point(LocalDate.of(2026, 2, 15), "0.4"), point(FIN_2026, "1"))));

        assertThat(lignes).hasSize(2);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes).extracting(l -> texte(l, DESCRIPTION)).containsExactly(DECOMPTE_N + 1, DECOMPTE_N + 2);
            s.assertThat(lignes).extracting(l -> texte(l, DATE)).as("début + 90 j × PointTime")
                    .containsExactly("2026-02-15", FIN_2026.toString());
            s.assertThat(lignes).extracting(l -> nombre(l, MONTANT).doubleValue()).containsExactly(480.0, 720.0);
            s.assertThat(lignes).extracting(l -> nombre(l, HT).doubleValue()).containsExactly(400.0, 600.0);
            s.assertThat(lignes).extracting(l -> nombre(l, TTC).doubleValue()).containsExactly(480.0, 720.0);
            s.assertThat(lignes).extracting(l -> nombre(l, AVANCEMENT).doubleValue()).containsExactly(0.4, 1.0);
        });
    }

    // ------------------------------------------------------------------ P5 : avance prévisionnelle

    /**
     * Avance 10 %, aucune pièce d'avance : reste = {@code BaseAdvanceAmount} = 1200 × 0,1 = 120, datée de la signature
     * (après le début), −1. Décomptes : 400 − 40 d'avance = 360 ; Σ = 120 + 3 × 360 = 1200.
     */
    @Test
    void p5_avancePrevisionnelle_moinsUn_datesDeSignature() throws Exception {
        UUID lot = lot(ENTREPRENEURS, "0.1", "0", "0");
        prev.signature(lot, LocalDate.of(2026, 1, 10));

        List<Map<String, Object>> lignes = recalculer(lot, inchange());

        List<Map<String, Object>> avances = avecLibelle(lignes, "Avance prévisionnelle");
        assertThat(avances).hasSize(1);
        Map<String, Object> avance = avances.getFirst();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(texte(avance, DATE)).isEqualTo("2026-01-10");
            s.assertThat(nombre(avance, MONTANT)).isCloseTo(new BigDecimal("120"), within(CENTIME));
            s.assertThat(nombre(avance, AVANCEMENT)).isEqualByComparingTo("-1");
            entite(s, avances, ENTITE_AVANCE);
            s.assertThat(avecLibelle(lignes, DECOMPTE_N)).hasSize(3).allSatisfy(l -> assertThat(nombre(l, MONTANT))
                    .isCloseTo(new BigDecimal("360"), within(CENTIME)));
            s.assertThat(somme(lignes, MONTANT)).isCloseTo(new BigDecimal("1200"), within(CENTIME));
        });
    }

    // ------------------------------------------------------------------ P9 : retenue de garantie

    /** RG 10 % : décomptes 400 − 40 = 360 ; RG 120 remboursée à fin (01/04) + 2 mois = 01/06, −1. */
    @Test
    void p9_rgSansEcheance_uneLigneAFinPlusMoisDeGarantie() throws Exception {
        List<Map<String, Object>> lignes = recalculer(lot(ENTREPRENEURS, "0", "0.1", "2"), inchange());

        List<Map<String, Object>> rg = avecLibelle(lignes, RG);
        assertThat(rg).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(texte(rg.getFirst(), DATE)).isEqualTo("2026-06-01");
            s.assertThat(nombre(rg.getFirst(), MONTANT)).isCloseTo(new BigDecimal("120"), within(CENTIME));
            s.assertThat(nombre(rg.getFirst(), AVANCEMENT)).isEqualByComparingTo("-1");
            entite(s, rg, ENTITE_RG);
            s.assertThat(avecLibelle(lignes, DECOMPTE_N)).hasSize(3).allSatisfy(l -> assertThat(nombre(l, MONTANT))
                    .isCloseTo(new BigDecimal("360"), within(CENTIME)));
        });
    }

    @Test
    void p9_rgBloquee_aucuneLigneDeRemboursement() throws Exception {
        UUID lot = lot(ENTREPRENEURS, "0", "0.1", "2");
        prev.rgBloquee(lot);

        List<Map<String, Object>> lignes = recalculer(lot, inchange());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(avecLibelle(lignes, RG)).isEmpty();
            s.assertThat(avecLibelle(lignes, DECOMPTE_N)).hasSize(3);
        });
    }

    // ------------------------------------------------------------------ P11 : multi-fournisseurs

    /** Multi, aucun BC : « Facture prévisionnel N° k » = 1200 × 1/3, HT 1000 × 1/3, {@code Achievement} 0, sans retenue. */
    @Test
    void p11_multiFournisseurs_facturesPrevisionnelles() throws Exception {
        List<Map<String, Object>> lignes = recalculer(lot(FOURNISSEURS, "0.1", "0.1", "1"), inchange());

        assertThat(lignes).extracting(l -> texte(l, DESCRIPTION))
                .containsExactly("Facture prévisionnel N° 1", "Facture prévisionnel N° 2", "Facture prévisionnel N° 3");
        SoftAssertions.assertSoftly(s -> {
            for (Map<String, Object> l : lignes) {
                s.assertThat(nombre(l, MONTANT)).isCloseTo(TIERS_TTC, within(CENTIME));
                s.assertThat(nombre(l, TTC)).isCloseTo(TIERS_TTC, within(CENTIME));
                s.assertThat(nombre(l, HT)).isCloseTo(TIERS_HT, within(CENTIME));
                s.assertThat(nombre(l, AVANCEMENT).doubleValue()).isCloseTo(0.0, within(PRECISION));
            }
            entite(s, lignes, ENTITE_FACTURE_MULTI);
        });
    }
}
