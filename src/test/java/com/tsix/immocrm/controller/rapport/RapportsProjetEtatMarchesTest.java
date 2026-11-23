package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R1, R2 (colonnes hors reste à payer / R.G. prévue / sur estimations / ratio) et D6 du rapport {@code PrjMarketTresorie}
 * « État des marchés » (FEATURE-n5b-c-etat-marches.md). Chaque test crée ses propres types et catégories (libellés à jeton)
 * et ne lit que les lignes de ces libellés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetEtatMarchesTest {

    private static final String ETAT = "PrjMarketTresorie";
    private static final BigDecimal PRECISION = new BigDecimal("0.01");
    private static final LocalDate JOUR = LocalDate.of(2026, 3, 5);

    private static final String TYPE_LOT = "typeLot";
    private static final String CATEGORIE = "categorie";
    private static final String SANS_TYPE = "(sans type)";
    private static final String ESTIMATION_BASE = "estimationBase";
    private static final String ESTIMATION_BASE_M2 = "estimationBaseM2";
    private static final String MARCHE = "marche";
    private static final String SUPP_AVENANTS = "supplementairesAvenants";
    private static final String RATIO_SUPP = "ratioSupplementaires";
    private static final String TOTAL_MARCHE = "totalMarche";
    private static final String TOTAL_MARCHE_M2 = "totalMarcheM2";
    private static final String ECART = "ecart";
    private static final String PAYE = "paye";
    private static final String RESTE_A_PAYER = "resteAPayer";
    private static final String RETENUE_GARANTIE = "retenueGarantie";
    private static final String RETENUE_RESTITUEE = "retenueRestituee";
    private static final String RETENUE_RESTE = "retenueResteARestituer";
    private static final String TRAVAUX_NON_PAYES = "travauxNonPayes";
    private static final String RESTE_MARCHES = "resteMarches";
    private static final String RESTE_ESTIMATIONS = "resteEstimations";
    private static final String RESTE_TOTAL = "resteTotal";
    /** Colonnes totalisées (R1 : toutes sauf {@code ecart} et {@code ratioSupplementaires}). */
    private static final List<String> TOTALISEES = List.of(ESTIMATION_BASE, ESTIMATION_BASE_M2, MARCHE, SUPP_AVENANTS,
            TOTAL_MARCHE, TOTAL_MARCHE_M2, PAYE, RESTE_A_PAYER, RETENUE_GARANTIE, RETENUE_RESTITUEE, RETENUE_RESTE,
            TRAVAUX_NON_PAYES, RESTE_MARCHES, RESTE_ESTIMATIONS, RESTE_TOTAL);

    private static final String TYPE_A = "A";
    private static final String TYPE_B = "B";
    private static final String CAT_X = "X";
    private static final String CAT_Y = "Y";
    private static final String CAT_Z = "Z";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String SUIVI = "/suivi";

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

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        c = RapportsProjetSupport.cadre(achat, comptes, fixture, parite, fiche);
        rapports.rapport(FixtureRapportsProjet.TYPE_PROJET, ETAT, "BILAN", "PROJET", 7);
        rapports.relire();
        s = new RapportsProjetBilanSupport(c, achat, fixture, parite, bilans, lots, jdbc, em);
    }

    // ------------------------------------------------------------------ outils

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private String libelleType(String nom) {
        return "Type " + nom + " " + c.jeton();
    }

    private UUID type(String nom) {
        return achat.typeLot(c.code("TL" + nom), libelleType(nom));
    }

    /** Lot entrepreneur du projet, de type et de catégorie donnés ({@code type} nul = sans type). */
    private UUID lot(UUID projet, String code, UUID type, UUID categorie) {
        UUID lot = s.lot(projet, code);
        if (type != null) {
            fixture.marche(lot, "PrjMarketTypeId", type);
        }
        fixture.marche(lot, "StlCategoryId", categorie);
        return lot;
    }

    private UUID categorie(String nom) {
        return s.categorie(nom, null, 1);
    }

    private Map<String, Object> section(UUID... projets) throws Exception {
        return premiere(appels.sections(requete(ETAT, c.compte(), projets)));
    }

    /** La ligne (type, catégorie) de libellés donnés (exactement une exigée). */
    private Map<String, Object> ligne(Map<String, Object> section, String typeLot, String categorie) {
        List<Map<String, Object>> l = AppelsRapports.lignesOu(section, CATEGORIE, categorie).stream()
                .filter(x -> typeLot.equals(x.get(TYPE_LOT))).toList();
        assertThat(l).as("ligne %s / %s", typeLot, categorie).hasSize(1);
        return l.getFirst();
    }

    private Map<String, Object> ligneAX(Map<String, Object> section) {
        return ligne(section, libelleType(TYPE_A), s.libelle(CAT_X));
    }

    private static void egal(SoftAssertions sa, Map<String, Object> ligne, String colonne, String attendu) {
        sa.assertThat(montant(ligne, colonne)).as(colonne).isCloseTo(new BigDecimal(attendu), within(PRECISION));
    }

    private void bordereau(UUID lot, String code, String quantite, String puHT) {
        bilans.ligneDefinitive(lot, c.code(code), quantite, puHT);
    }

    /** Témoin : corps JSON de {@code /api/projet/lots/{id}{suffixe}} lu par un administrateur du projet (200 exigé). */
    private String fiche(UUID lot, UUID projet, String suffixe) throws Exception {
        return mockMvc.perform(get("/api/projet/lots/" + lot + suffixe)
                        .with(connecte(parite.administrateur("n5c.adm." + c.jeton(), projet), ADMINISTRATEUR)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal lire(String corps, String chemin) {
        return AppelsRapports.nombre(JsonPath.read(corps, chemin));
    }

    /** Ligne {@code PrjWorkingOutLineExtra} (FODECRatio 0 : la vue ne fait pas ISNULL). */
    private void extra(UUID decompte, String quantite, String puHT) {
        BigDecimal pu = new BigDecimal(puHT);
        maj("""
                INSERT INTO PrjWorkingOutLineExtra (Pkey, Sorting, Description, Quantity, UnitPriceHT, VatRatio, UnitPriceTTC,
                    FODECRatio, PrjWorkingOutId) VALUES (?, 1, N'Extra', ?, ?, 0.2, ?, 0, ?)""",
                UUID.randomUUID().toString(), new BigDecimal(quantite), pu, pu.multiply(new BigDecimal("1.2")),
                decompte.toString());
    }

    private UUID trancheDeSurface(String code, UUID projet, String taux, String surface) {
        UUID tranche = bilans.tranche(code, projet);
        bilans.ventilationTranche(projet, tranche, taux);
        maj("UPDATE CfgTranche SET CoveredFloorArea = ? WHERE PKey = ?", new BigDecimal(surface), tranche.toString());
        return tranche;
    }

    // ------------------------------------------------------------------ R1 lignes, ordre, totaux

    @Test
    void r1_uneLigneParTypeEtCategorie_sommeDesLotsDuGroupe() throws Exception {
        UUID a = type(TYPE_A);
        UUID x = categorie(CAT_X);
        UUID y = categorie(CAT_Y);
        s.estimationBase(lot(c.pa1(), "L1", a, x), "1000");
        s.estimationBase(lot(c.pa1(), "L2", a, x), "500");
        s.estimationBase(lot(c.pa1(), "L3", a, y), "100");

        Map<String, Object> r = section(c.pa1());

        SoftAssertions sa = new SoftAssertions();
        egal(sa, ligneAX(r), ESTIMATION_BASE, "1800");
        egal(sa, ligne(r, libelleType(TYPE_A), s.libelle(CAT_Y)), ESTIMATION_BASE, "120");
        sa.assertAll();
    }

    @Test
    void r1_ordre_typePuisCategorie_parLibelle() throws Exception {
        UUID a = type(TYPE_A);
        UUID b = type(TYPE_B);
        UUID y = categorie(CAT_Y);
        UUID z = categorie(CAT_Z);
        s.estimationBase(lot(c.pa1(), "O1", b, y), "10");
        s.estimationBase(lot(c.pa1(), "O2", a, z), "10");
        s.estimationBase(lot(c.pa1(), "O3", a, y), "10");
        Set<String> types = Set.of(libelleType(TYPE_A), libelleType(TYPE_B));

        List<String> ordre = AppelsRapports.lignes(section(c.pa1())).stream()
                .filter(l -> types.contains(l.get(TYPE_LOT)))
                .map(l -> l.get(TYPE_LOT) + " | " + l.get(CATEGORIE)).toList();

        assertThat(ordre).containsExactly(
                libelleType(TYPE_A) + " | " + s.libelle(CAT_Y),
                libelleType(TYPE_A) + " | " + s.libelle(CAT_Z),
                libelleType(TYPE_B) + " | " + s.libelle(CAT_Y));
    }

    @Test
    void r1_lotSansType_ligneSansType() throws Exception {
        UUID x = categorie(CAT_X);
        s.estimationBase(lot(c.pa1(), "LS", null, x), "1000");

        Map<String, Object> l = ligne(section(c.pa1()), SANS_TYPE, s.libelle(CAT_X));

        assertThat(montant(l, ESTIMATION_BASE)).isCloseTo(new BigDecimal("1200"), within(PRECISION));
    }

    @Test
    void r1_totaux_sommeDesLignes_sansTotalEcartNiRatio() throws Exception {
        UUID a = type(TYPE_A);
        UUID b = type(TYPE_B);
        UUID x = categorie(CAT_X);
        UUID l1 = lot(c.pa1(), "T1", a, x);
        s.estimationBase(l1, "1000");
        bordereau(l1, "T1B", "3", "500");
        UUID l2 = lot(c.pa1(), "T2", b, x);
        s.estimationBase(l2, "500");
        bordereau(l2, "T2B", "1", "1000");

        Map<String, Object> r = section(c.pa1());
        Map<String, Object> totaux = AppelsRapports.totaux(r);
        List<Map<String, Object>> lignes = AppelsRapports.lignes(r);

        assertThat(totaux).as("totaux").isNotNull();
        SoftAssertions sa = new SoftAssertions();
        for (String colonne : TOTALISEES) {
            sa.assertThat(montant(totaux, colonne)).as("total %s", colonne)
                    .isCloseTo(AppelsRapports.somme(lignes, colonne), within(PRECISION));
        }
        egal(sa, totaux, ESTIMATION_BASE, "1800");
        egal(sa, totaux, MARCHE, "3000");
        sa.assertThat(AppelsRapports.somme(lignes, ECART)).as("écart non nul sur les lignes")
                .isCloseTo(new BigDecimal("1200"), within(PRECISION));
        egal(sa, totaux, ECART, "0");
        egal(sa, totaux, RATIO_SUPP, "0");
        sa.assertAll();
    }

    // ------------------------------------------------------------------ R2 colonnes

    @Test
    void r2_estimationBase_marche_ecart_commeLaFicheLot() throws Exception {
        UUID lot = lot(c.pa1(), "LE", type(TYPE_A), categorie(CAT_X));
        s.estimationBase(lot, "1000");
        bordereau(lot, "LEB", "3", "500");
        String corps = fiche(lot, c.pa1(), "");
        SoftAssertions temoin = new SoftAssertions();
        temoin.assertThat(lire(corps, "$.general.estimationBase.ttc")).as("témoin fiche estimation")
                .isCloseTo(new BigDecimal("1200"), within(PRECISION));
        temoin.assertThat(lire(corps, "$.general.marcheAjuste.ttc")).as("témoin fiche marché")
                .isCloseTo(new BigDecimal("1800"), within(PRECISION));
        temoin.assertAll();

        Map<String, Object> l = ligneAX(section(c.pa1()));

        SoftAssertions sa = new SoftAssertions();
        egal(sa, l, ESTIMATION_BASE, "1200");
        egal(sa, l, MARCHE, "1800");
        egal(sa, l, ECART, "600");
        sa.assertAll();
    }

    /** Q1 : supplémentaires = lignes extra des décomptes facturés seulement ; avenants = Σ PrjClause.AmountTTC. */
    @Test
    void r2_supplementairesAvenants_totalMarche_lotMono() throws Exception {
        UUID lot = lot(c.pa1(), "LA", type(TYPE_A), categorie(CAT_X));
        bordereau(lot, "LAB", "3", "500");
        fiche.avenant(lot, c.code("LAV"), "200", "40", "240");
        UUID facture = fixture.facture(lot, c.code("LA") + "-F", "0", "0", "0");
        UUID decompteFacture = fixture.decompte(lot, c.code("LA") + "-D1", 1, "0", "0", "0", JOUR);
        maj("UPDATE PrjWorkingOut SET BuyDocumentId = ? WHERE PKey = ?", facture.toString(), decompteFacture.toString());
        extra(decompteFacture, "2", "100");
        UUID nonFacture = fixture.decompte(lot, c.code("LA") + "-D2", 2, "0", "0", "0", JOUR);
        extra(nonFacture, "1", "1000");
        String suivi = fiche(lot, c.pa1(), SUIVI);
        SoftAssertions temoin = new SoftAssertions();
        temoin.assertThat(lire(suivi, "$.colonnes.factures.travauxSuppTTC")).as("témoin supp. facturés")
                .isCloseTo(new BigDecimal("240"), within(PRECISION));
        temoin.assertThat(lire(suivi, "$.colonnes.decomptes.avenantsTTC")).as("témoin avenants")
                .isCloseTo(new BigDecimal("240"), within(PRECISION));
        temoin.assertAll();

        Map<String, Object> l = ligneAX(section(c.pa1()));

        SoftAssertions sa = new SoftAssertions();
        egal(sa, l, SUPP_AVENANTS, "480");
        egal(sa, l, TOTAL_MARCHE, "2280");
        sa.assertAll();
    }

    /** Q3 : payé = Σ des lignes de règlement de toutes les pièces du lot, pièce annulée comprise. */
    @Test
    void r2_paye_compteLeReglementDUnePieceAnnulee() throws Exception {
        UUID lot = lot(c.pa1(), "LP", type(TYPE_A), categorie(CAT_X));
        UUID valide = s.piece(lot, c.pa1(), "LPF", "1000", "1000", true);
        s.regler(valide, "LPR", "300");
        UUID annulee = s.piece(lot, c.pa1(), "LPA", "500", "500", true);
        achat.poser(annulee, "BuyDocumentStatusId",
                achat.statutDEtat(c.code("STAN"), FixtureAchat.ETAT_ANNULATION, null));
        s.regler(annulee, "LPAR", "200");

        Map<String, Object> l = ligneAX(section(c.pa1()));

        assertThat(montant(l, PAYE)).isCloseTo(new BigDecimal("500"), within(PRECISION));
    }

    @Test
    void r2_travauxNonPayes_retainedAmountDesFactures() throws Exception {
        UUID lot = lot(c.pa1(), "LN", type(TYPE_A), categorie(CAT_X));
        UUID facture = fixture.facture(lot, c.code("LN") + "-F", "0", "0", "0");
        achat.poser(facture, "RetainedAmount", new BigDecimal("50"));

        Map<String, Object> l = ligneAX(section(c.pa1()));

        assertThat(montant(l, TRAVAUX_NON_PAYES)).isCloseTo(new BigDecimal("50"), within(PRECISION));
    }

    /** R.G. décomptée 100, facturée 80, restitution pénalité 10 réglée 30 : restituée 30, reste 80 − 10 − 30 = 40. */
    @Test
    void r2_retenueRestituee_resteARestituer_commeLeSuiviDuLot() throws Exception {
        UUID lot = lotAvecRestitution("LR");
        String suivi = fiche(lot, c.pa1(), SUIVI);
        SoftAssertions temoin = new SoftAssertions();
        temoin.assertThat(lire(suivi, "$.retenueGarantie.restituee")).as("témoin restituée")
                .isCloseTo(new BigDecimal("30"), within(PRECISION));
        temoin.assertThat(lire(suivi, "$.retenueGarantie.resteARestituer")).as("témoin reste")
                .isCloseTo(new BigDecimal("40"), within(PRECISION));
        temoin.assertAll();

        Map<String, Object> l = ligneAX(section(c.pa1()));

        SoftAssertions sa = new SoftAssertions();
        egal(sa, l, RETENUE_RESTITUEE, "30");
        egal(sa, l, RETENUE_RESTE, "40");
        sa.assertAll();
    }

    @Test
    void r2_resteARestituer_blocage_zero() throws Exception {
        UUID lot = lotAvecRestitution("LB");
        fixture.marche(lot, "BlokWarrantyRestitution", true);
        assertThat(lire(fiche(lot, c.pa1(), SUIVI), "$.retenueGarantie.resteARestituer")).as("témoin reste bloqué")
                .isCloseTo(BigDecimal.ZERO, within(PRECISION));

        Map<String, Object> l = ligneAX(section(c.pa1()));

        assertThat(montant(l, RETENUE_RESTE)).isCloseTo(BigDecimal.ZERO, within(PRECISION));
    }

    private UUID lotAvecRestitution(String code) {
        UUID lot = lot(c.pa1(), code, type(TYPE_A), categorie(CAT_X));
        fixture.fournisseur(lot, c.fournisseur());
        fixture.decompte(lot, c.code(code) + "-D", 1, "100", "0", "0", JOUR);
        fixture.facture(lot, c.code(code) + "-F", "80", "0", "0");
        fixture.restitution(lot, c.code(code) + "-RS", "-10", "30");
        return lot;
    }

    // ------------------------------------------------------------------ m² et D6

    /** Groupe sur 2 projets : PA1 surface 300 (1200 → 4 ; marché 1800 → 6), PA2 surface 100 (600 → 6 ; 600 → 6) ; Σ par ligne. */
    @Test
    void r2_prixM2_parLigneLotTranche_sommeesDansLeGroupe() throws Exception {
        bilans.preparer();
        trancheDeSurface("M1", c.pa1(), "1", "300");
        trancheDeSurface("M2", c.pa2(), "1", "100");
        UUID a = type(TYPE_A);
        UUID x = categorie(CAT_X);
        UUID l1 = lot(c.pa1(), "M1", a, x);
        s.estimationBase(l1, "1000");
        bordereau(l1, "M1B", "3", "500");
        UUID l2 = lot(c.pa2(), "M2", a, x);
        s.estimationBase(l2, "500");
        bordereau(l2, "M2B", "1", "500");

        Map<String, Object> l = ligneAX(section(c.pa1(), c.pa2()));

        SoftAssertions sa = new SoftAssertions();
        egal(sa, l, ESTIMATION_BASE_M2, "10");
        egal(sa, l, TOTAL_MARCHE_M2, "12");
        sa.assertAll();
    }

    @Test
    void r2_prixM2_zeroSansTrancheOuSurfaceNulle() throws Exception {
        bilans.preparer();
        trancheDeSurface("Z2", c.pa2(), "1", "0");
        UUID a = type(TYPE_A);
        UUID sansTranche = lot(c.pa1(), "Z1", a, categorie(CAT_X));
        s.estimationBase(sansTranche, "1000");
        bordereau(sansTranche, "Z1B", "1", "500");
        UUID surfaceNulle = lot(c.pa2(), "Z2", a, categorie(CAT_Y));
        s.estimationBase(surfaceNulle, "1000");
        bordereau(surfaceNulle, "Z2B", "1", "500");

        Map<String, Object> r = section(c.pa1(), c.pa2());
        Map<String, Object> lx = ligneAX(r);
        Map<String, Object> ly = ligne(r, libelleType(TYPE_A), s.libelle(CAT_Y));

        SoftAssertions sa = new SoftAssertions();
        egal(sa, lx, ESTIMATION_BASE, "1200");
        egal(sa, lx, ESTIMATION_BASE_M2, "0");
        egal(sa, lx, TOTAL_MARCHE_M2, "0");
        egal(sa, ly, ESTIMATION_BASE, "1200");
        egal(sa, ly, ESTIMATION_BASE_M2, "0");
        egal(sa, ly, TOTAL_MARCHE_M2, "0");
        sa.assertAll();
    }

    /** D6 : projet ventilé sur 2 tranches à 0,5 et 0,3 → chaque montant du lot × 0,8 dans le groupe. */
    @Test
    void d6_deuxTranches_montantsFoisTauxDeVentilation() throws Exception {
        bilans.preparer();
        trancheDeSurface("V1", c.pa1(), "0.5", "100");
        trancheDeSurface("V2", c.pa1(), "0.3", "100");
        UUID lot = lot(c.pa1(), "LV", type(TYPE_A), categorie(CAT_X));
        s.estimationBase(lot, "1000");
        bordereau(lot, "LVB", "3", "500");
        UUID piece = s.piece(lot, c.pa1(), "LVF", "1000", "1000", true);
        s.regler(piece, "LVR", "500");

        Map<String, Object> l = ligneAX(section(c.pa1()));

        SoftAssertions sa = new SoftAssertions();
        egal(sa, l, ESTIMATION_BASE, "960");
        egal(sa, l, MARCHE, "1440");
        egal(sa, l, PAYE, "400");
        egal(sa, l, ECART, "480");
        sa.assertAll();
    }
}
