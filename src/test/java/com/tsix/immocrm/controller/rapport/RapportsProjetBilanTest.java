package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.A_PAYER;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.BILAN;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.CATEGORIE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.CUMUL_TRAVAUX;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.ESTIMATION_BASE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.ESTIMATION_CONFIDENTIELLE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.FAISABILITE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.MARCHE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.MONTANTS;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.NIVEAU;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.REGLE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetBilanSupport.RESTE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * R1 {@code PrjTdbBilan} (FEATURE-n5b-b-rapports-projet.md). Lot L de PA1 ventilé K1 0,25 / K2 0,75 sous la racine K :
 * estimation de base 1000 HT (1200 TTC), estimation confidentielle 500 HT (600 TTC), devis retenu 2400 TTC, facture
 * payable TTC 1000 / à payer 800 ventilée K1 0,4 / K2 0,6, réglée 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetBilanTest {

    private static final BigDecimal PRECISION = new BigDecimal("0.01");
    private static final String K = "K";
    private static final String K1 = "K1";
    private static final String K2 = "K2";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat comptes;
    @Autowired private FixtureRapportsProjet fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureBilansProjet bilans;
    @Autowired private FixtureProjetLots lots;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private AppelsRapports appels;
    private Cadre c;
    private RapportsProjetBilanSupport s;
    private UUID k1;
    private UUID lot;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        c = RapportsProjetSupport.cadre(achat, comptes, fixture, parite, fiche);
        RapportsProjetBilanSupport.rapports(rapports);
        s = new RapportsProjetBilanSupport(c, achat, fixture, parite, bilans, lots, jdbc, em);
        UUID k = s.categorie(K, null, 1);
        k1 = s.categorie(K1, k, 1);
        UUID k2 = s.categorie(K2, k, 2);
        lot = s.lot(c.pa1(), "L");
        s.ventiler(lot, k1, "0.25");
        s.ventiler(lot, k2, "0.75");
        s.estimationBase(lot, "1000");
        s.estimationConfidentielle(lot, "LE", "500");
        s.marche(lot, c.pa1(), "LD", "2400");
        bilans.ligneDefinitive(lot, "LB", "2", "500");
        UUID piece = s.piece(lot, c.pa1(), "LF", "1000", "800", true);
        s.ventilerPiece(piece, k1, "0.4");
        s.ventilerPiece(piece, k2, "0.6");
        s.regler(piece, "LR", "500");
    }

    private Map<String, Object> section(UUID... projets) throws Exception {
        return premiere(appels.sections(requete(BILAN, c.compte(), projets)));
    }

    /** La ligne de la catégorie de libellé donné (exactement une exigée). */
    private Map<String, Object> ligne(Map<String, Object> section, String nom) {
        List<Map<String, Object>> l = AppelsRapports.lignesOu(section, CATEGORIE, s.libelle(nom));
        assertThat(l).as("ligne %s", s.libelle(nom)).hasSize(1);
        return l.getFirst();
    }

    private List<Object> libelles(Map<String, Object> section) {
        return AppelsRapports.lignes(section).stream().map(l -> l.get(CATEGORIE)).toList();
    }

    private static void egal(SoftAssertions sa, Map<String, Object> ligne, String colonne, String attendu) {
        sa.assertThat(montant(ligne, colonne)).as("%s de %s", colonne, ligne.get(CATEGORIE))
                .isCloseTo(new BigDecimal(attendu), within(PRECISION));
    }

    /** K1, K2, K pour une colonne. */
    private void colonne(String nom, String k1Attendu, String k2Attendu, String kAttendu) throws Exception {
        Map<String, Object> r = section(c.pa1());
        SoftAssertions sa = new SoftAssertions();
        egal(sa, ligne(r, K1), nom, k1Attendu);
        egal(sa, ligne(r, K2), nom, k2Attendu);
        egal(sa, ligne(r, K), nom, kAttendu);
        sa.assertAll();
    }

    // ------------------------------------------------------------------ arbre

    @Test
    void r1_arbre_parcoursPrefixe_ordreSorting_niveaux() throws Exception {
        UUID a = s.categorie("A", null, 2);
        UUID b = s.categorie("B", null, 1);
        UUID b1 = s.categorie("B1", b, 2);
        UUID b2 = s.categorie("B2", b, 1);
        UUID a1 = s.categorie("A1", a, 1);
        UUID l2 = s.lot(c.pa2(), "L2");
        s.ventiler(l2, b1, "0.3");
        s.ventiler(l2, b2, "0.2");
        s.ventiler(l2, a1, "0.5");
        s.estimationBase(l2, "1000");

        Map<String, Object> r = section(c.pa2());

        assertThat(libelles(r)).containsExactly(s.libelle("B"), s.libelle("B2"), s.libelle("B1"), s.libelle("A"),
                s.libelle("A1"));
        assertThat(AppelsRapports.lignes(r).stream().map(l -> AppelsRapports.nombre(l.get(NIVEAU)).intValue()).toList())
                .containsExactly(0, 1, 1, 0, 1);
    }

    @Test
    void r1_parent_egalSommeDeSesDescendants_surTroisNiveaux() throws Exception {
        UUID racine = s.categorie("R", null, 1);
        UUID e1 = s.categorie("E1", racine, 1);
        UUID e2 = s.categorie("E2", racine, 2);
        UUID g = s.categorie("G", e2, 1);
        UUID l2 = s.lot(c.pa2(), "L3");
        s.ventiler(l2, e1, "0.6");
        s.ventiler(l2, g, "0.4");
        s.estimationBase(l2, "1000");
        UUID piece = s.piece(l2, c.pa2(), "L3F", "100", "100", true);
        s.ventilerPiece(piece, g, "1");

        Map<String, Object> r = section(c.pa2());

        SoftAssertions sa = new SoftAssertions();
        egal(sa, ligne(r, "E1"), ESTIMATION_BASE, "720");
        egal(sa, ligne(r, "G"), ESTIMATION_BASE, "480");
        egal(sa, ligne(r, "E2"), ESTIMATION_BASE, "480");
        egal(sa, ligne(r, "R"), ESTIMATION_BASE, "1200");
        egal(sa, ligne(r, "E2"), CUMUL_TRAVAUX, "100");
        egal(sa, ligne(r, "R"), CUMUL_TRAVAUX, "100");
        sa.assertThat(AppelsRapports.nombre(ligne(r, "G").get(NIVEAU)).intValue()).as("niveau G").isEqualTo(2);
        sa.assertAll();
    }

    @Test
    void r1_categorieSansMontant_absente_ainsiQueSonParent() throws Exception {
        UUID vide = s.categorie("V", null, 3);
        s.categorie("V1", vide, 1);
        UUID z = s.categorie("Z", null, 4);
        UUID lotNul = s.lot(c.pa1(), "LZ");
        s.ventiler(lotNul, z, "1");

        assertThat(libelles(section(c.pa1())))
                .contains(s.libelle(K), s.libelle(K1))
                .doesNotContain(s.libelle("V"), s.libelle("V1"), s.libelle("Z"));
    }

    // ------------------------------------------------------------------ une formule par colonne

    @Test
    void r1_estimationBase_ttcManuel_foisTauxDeVentilation() throws Exception {
        colonne(ESTIMATION_BASE, "300", "900", "1200");
    }

    @Test
    void r1_estimationConfidentielle_estimatedPriceTtc_foisTaux() throws Exception {
        colonne(ESTIMATION_CONFIDENTIELLE, "150", "450", "600");
    }

    @Test
    void r1_marche_marcheAjusteTtc_foisTaux() throws Exception {
        // bordereau 2 × 500 HT = 1200 TTC, différent du devis retenu (2400)
        colonne(MARCHE, "300", "900", "1200");
    }

    @Test
    void r1_cumulTravaux_amountTtcDesPiecesPayables_foisVentilationPiece() throws Exception {
        colonne(CUMUL_TRAVAUX, "400", "600", "1000");
    }

    @Test
    void r1_aPayer_amountToPay_foisVentilationPiece() throws Exception {
        colonne(A_PAYER, "320", "480", "800");
    }

    @Test
    void r1_regle_lignesDeReglement_foisVentilationPiece() throws Exception {
        colonne(REGLE, "200", "300", "500");
    }

    @Test
    void r1_reste_aPayerMoinsRegle() throws Exception {
        colonne(RESTE, "120", "180", "300");
    }

    @Test
    void r1_pieceCanBePayedFaux_ignoree_dansCumulAPayerRegle() throws Exception {
        UUID ignoree = s.piece(lot, c.pa1(), "LX", "5000", "5000", false);
        s.ventilerPiece(ignoree, k1, "1");
        s.regler(ignoree, "LXR", "1000");

        Map<String, Object> r = section(c.pa1());

        SoftAssertions sa = new SoftAssertions();
        egal(sa, ligne(r, K1), CUMUL_TRAVAUX, "400");
        egal(sa, ligne(r, K1), A_PAYER, "320");
        egal(sa, ligne(r, K1), REGLE, "200");
        sa.assertAll();
    }

    @Test
    void r1_pieceSansLot_ignoree() throws Exception {
        UUID sansLot = s.piece(null, c.pa1(), "LS", "7000", "7000", true);
        s.ventilerPiece(sansLot, k1, "1");

        Map<String, Object> r = section(c.pa1());

        SoftAssertions sa = new SoftAssertions();
        egal(sa, ligne(r, K1), CUMUL_TRAVAUX, "400");
        egal(sa, ligne(r, K1), A_PAYER, "320");
        sa.assertAll();
    }

    /** D5 : estimation 1200, confidentielle 600, devis 2400, mais lot résilié → les 3 colonnes = cumul travaux 900. */
    @Test
    void r1_lotResilie_troisColonnesEstimationMarche_egalesCumulTravaux() throws Exception {
        UUID x = s.categorie("X", null, 5);
        UUID resilie = s.lotResilie(c.pa2(), "LR2");
        s.ventiler(resilie, x, "1");
        s.estimationBase(resilie, "1000");
        s.estimationConfidentielle(resilie, "LR2E", "500");
        s.marche(resilie, c.pa2(), "LR2D", "2400");
        UUID piece = s.piece(resilie, c.pa2(), "LR2F", "900", "900", true);
        s.ventilerPiece(piece, x, "1");

        Map<String, Object> ligneX = ligne(section(c.pa2()), "X");

        SoftAssertions sa = new SoftAssertions();
        egal(sa, ligneX, CUMUL_TRAVAUX, "900");
        egal(sa, ligneX, ESTIMATION_BASE, "900");
        egal(sa, ligneX, ESTIMATION_CONFIDENTIELLE, "900");
        egal(sa, ligneX, MARCHE, "900");
        sa.assertAll();
    }

    // ------------------------------------------------------------------ totaux, projets, faisabilité

    @Test
    void r1_totaux_sommeDesRacinesSeulement() throws Exception {
        Map<String, Object> r = section(c.pa1());
        Map<String, Object> totaux = AppelsRapports.totaux(r);
        List<Map<String, Object>> racines = AppelsRapports.lignes(r).stream()
                .filter(l -> AppelsRapports.nombre(l.get(NIVEAU)).intValue() == 0).toList();

        assertThat(totaux).as("totaux").isNotNull();
        SoftAssertions sa = new SoftAssertions();
        for (String colonne : MONTANTS) {
            sa.assertThat(montant(totaux, colonne)).as("total %s", colonne)
                    .isCloseTo(AppelsRapports.somme(racines, colonne), within(PRECISION));
        }
        sa.assertThat(montant(totaux, ESTIMATION_BASE)).as("pas de double compte K + K1 + K2")
                .isCloseTo(new BigDecimal("1200"), within(PRECISION));
        sa.assertThat(montant(totaux, CUMUL_TRAVAUX)).isCloseTo(new BigDecimal("1000"), within(PRECISION));
        sa.assertAll();
    }

    @Test
    void r1_sansProjets_tousLesProjetsHabilites() throws Exception {
        UUID l2 = s.lot(c.pa2(), "LP2");
        s.ventiler(l2, k1, "1");
        s.estimationBase(l2, "100");

        SoftAssertions sa = new SoftAssertions();
        egal(sa, ligne(section(c.pa1()), K1), ESTIMATION_BASE, "300");
        egal(sa, ligne(section(), K1), ESTIMATION_BASE, "420");
        sa.assertAll();
    }

    @Test
    void r1_colonnes_dontEtudeFaisabilite_zeroSansEtude() throws Exception {
        Map<String, Object> r = section(c.pa1());
        List<Object> noms = AppelsRapports.colonnes(r).stream().map(col -> col.get("nom")).toList();

        assertThat(noms).contains(CATEGORIE, NIVEAU).containsAll(MONTANTS);
        assertThat(montant(ligne(r, K), FAISABILITE)).isCloseTo(BigDecimal.ZERO, within(PRECISION));
    }
}
