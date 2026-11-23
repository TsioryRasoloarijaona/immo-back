package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
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
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * R3-R5 de {@code PrjMarketTresorie} (FEATURE-n5b-c-etat-marches.md, « Formules sur tables ») : chaque test bâtit un lot
 * mono-fournisseur d'un type de lot propre (une ligne du rapport) qui n'exerce qu'un terme de la formule. Ratios en fraction,
 * TVA 0,2, FODEC 0 ; bordereau de base 10 × 100 HT = 1200 TTC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetEtatMarchesFormulesTest {

    private static final String ETAT = "PrjMarketTresorie";
    private static final String TYPE_LOT = "typeLot";
    private static final String RESTE_A_PAYER = "resteAPayer";
    private static final String RESTE_MARCHES = "resteMarches";
    private static final String RETENUE_GARANTIE = "retenueGarantie";
    private static final String RESTE_ESTIMATIONS = "resteEstimations";
    private static final String RESTE_TOTAL = "resteTotal";
    private static final String RATIO = "ratioSupplementaires";
    private static final String PAYE = "paye";
    private static final String TVA = "0.2";
    private static final String DIX = "10";
    private static final String CENT = "100";
    private static final String ZERO = "0";
    private static final String UN = "1";
    private static final String MILLE = "1000";
    private static final String UN_POUR_CENT = "0.01";
    private static final String DEUX_POUR_CENT = "0.02";
    private static final BigDecimal PRECISION = new BigDecimal("0.01");
    private static final BigDecimal PRECISION_RATIO = new BigDecimal("0.0001");
    private static final LocalDate JOUR = LocalDate.of(2026, 3, 1);

    // Colonnes lues par R3-R5 : présentes en prod et en CI, ajoutées si yesRemsTest les ignore (annulé au rollback).
    private static final String COLONNES = """
            IF COL_LENGTH('dbo.PrjMarket', 'InsuranceDECAmount') IS NULL ALTER TABLE dbo.PrjMarket ADD InsuranceDECAmount decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjMarket', 'InsuranceTRCAmount') IS NULL ALTER TABLE dbo.PrjMarket ADD InsuranceTRCAmount decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjMarket', 'WarrantyRatio') IS NULL ALTER TABLE dbo.PrjMarket ADD WarrantyRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjMarket', 'ApplyWithholdingTaxOnAdditionalWork') IS NULL
                ALTER TABLE dbo.PrjMarket ADD ApplyWithholdingTaxOnAdditionalWork bit NULL;
            IF COL_LENGTH('dbo.PrjProject', 'ManageProrataPrjMarketId') IS NULL
                ALTER TABLE dbo.PrjProject ADD ManageProrataPrjMarketId uniqueidentifier NULL;""";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat comptes;
    @Autowired private FixtureRapportsProjet fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureBilansProjet bilans;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureDecomptes decomptes;
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
        maj(COLONNES);
    }

    // ------------------------------------------------------------------ outils

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    /** Lot mono de PA1, type de lot et catégorie propres au test, ratios et montants de retenue à 0. */
    private UUID lot(String code) {
        UUID lot = s.lot(c.pa1(), code);
        typer(lot, type(code), s.categorie("C" + code, null, 1));
        taux(lot, ZERO, ZERO, ZERO, ZERO, ZERO);
        return lot;
    }

    private UUID type(String code) {
        return achat.typeLot(c.code("T" + code), s.libelle("T" + code));
    }

    private void typer(UUID lot, UUID type, UUID categorie) {
        maj("UPDATE PrjMarket SET PrjMarketTypeId = ?, StlCategoryId = ? WHERE PKey = ?", type.toString(), categorie.toString(),
                lot.toString());
    }

    /** Prorata, DEC (ratio, montant), TRC (ratio, montant) ; R.G. 0, retenues sur travaux sup. non appliquées. */
    private void taux(UUID lot, String prorata, String decRatio, String decMontant, String trcRatio, String trcMontant) {
        maj("""
                UPDATE PrjMarket SET ProportionRatio = ?, InsuranceDECRatio = ?, InsuranceDECAmount = ?, InsuranceTRCRatio = ?,
                    InsuranceTRCAmount = ?, WarrantyRatio = 0, ApplyWithholdingTaxOnAdditionalWork = 0 WHERE PKey = ?""",
                new BigDecimal(prorata), new BigDecimal(decRatio), new BigDecimal(decMontant), new BigDecimal(trcRatio),
                new BigDecimal(trcMontant), lot.toString());
    }

    /** Bordereau 10 × 100 HT = 1200 TTC ; rend la ligne de marché. */
    private UUID bordereau(UUID lot) {
        return decomptes.ligneMarche(lot, 1, DIX, CENT, TVA);
    }

    private UUID decompte(UUID lot, String code, int numero) {
        return fixture.decompte(lot, c.code(code), numero, ZERO, ZERO, ZERO, JOUR);
    }

    /** Facture de décompte rattachée par {@code PrjWorkingOut.BuyDocumentId}. */
    private void facturer(UUID lot, UUID decompte, String code) {
        UUID facture = fixture.facture(lot, c.code(code), ZERO, ZERO, ZERO);
        maj("UPDATE PrjWorkingOut SET BuyDocumentId = ? WHERE PKey = ?", facture.toString(), decompte.toString());
    }

    /** Ligne de décompte à 12 sur la ligne de marché à 10 : dépassement 2 × 120 = 240 TTC. */
    private void depassement(UUID decompte, UUID ligneMarche) {
        decomptes.ligne(decompte, ligneMarche, 1, "12", ZERO, CENT, TVA);
    }

    private Map<String, Object> ligne(String code) throws Exception {
        Map<String, Object> section = premiere(appels.sections(requete(ETAT, c.compte(), c.pa1())));
        List<Map<String, Object>> l = AppelsRapports.lignesOu(section, TYPE_LOT, s.libelle("T" + code));
        assertThat(l).as("ligne du type %s", s.libelle("T" + code)).hasSize(1);
        return l.getFirst();
    }

    private static void egal(SoftAssertions sa, Map<String, Object> ligne, String colonne, String attendu) {
        sa.assertThat(montant(ligne, colonne)).as(colonne).isCloseTo(new BigDecimal(attendu), within(PRECISION));
    }

    /** resteAPayer = resteMarches = attendu (rien de payé). */
    private void resteAPayer(String code, String attendu) throws Exception {
        Map<String, Object> l = ligne(code);
        SoftAssertions sa = new SoftAssertions();
        egal(sa, l, RESTE_A_PAYER, attendu);
        egal(sa, l, RESTE_MARCHES, attendu);
        sa.assertAll();
    }

    // ------------------------------------------------------------------ R3 DueAmountToPay − paye

    /** R3 OfferedAmountToPay : 1200 − (0,02 × 1200 + DEC 0,01 × 1200 + TRC montant 6 prioritaire sur son ratio 0,5) = 1158. */
    @Test
    void r3_offeredAmountToPay_marcheMoinsProrataDecTrc_montantPrioritaireSurRatio() throws Exception {
        UUID lot = lot("OF");
        bordereau(lot);
        taux(lot, DEUX_POUR_CENT, UN_POUR_CENT, ZERO, "0.5", "6");

        resteAPayer("OF", "1158");
    }

    /** R3 PrjWorkingOutTotalFluctuationsToPayAmount : bordereau 1200 + révision de prix 50 d'un décompte = 1250. */
    @Test
    void r3_fluctuationsDesDecomptes_ajoutees() throws Exception {
        UUID lot = lot("FL");
        bordereau(lot);
        UUID d = decompte(lot, "FLD", 1);
        maj("UPDATE PrjWorkingOut SET FluctuationsToPay = 50 WHERE PKey = ?", d.toString());

        resteAPayer("FL", "1250");
    }

    /**
     * R3 ExtraAmountToPay (décompté, non facturé) : extra 2 × 100 HT = 240 TTC, retenues sur travaux sup. appliquées
     * (prorata 0,02 + DEC 0,01 + TRC 0,01 = 4 % de 240) → 230,40 ; pas de bordereau.
     */
    @Test
    void r3_extraAmountToPay_extraDecompteMoinsRetenuesNonRemboursables() throws Exception {
        UUID lot = lot("EX");
        taux(lot, DEUX_POUR_CENT, UN_POUR_CENT, ZERO, UN_POUR_CENT, ZERO);
        fixture.marche(lot, "ApplyWithholdingTaxOnAdditionalWork", true);
        decomptes.extra(decompte(lot, "EXD", 1), "2", CENT, TVA);

        resteAPayer("EX", "230.40");
    }

    /** R3 OverAmountToPay : bordereau 1200 − prorata 5 % = 1140 ; dépassement 240 − 5 % = 228 → 1368. */
    @Test
    void r3_overAmountToPay_depassementMoinsRetenues() throws Exception {
        UUID lot = lot("OV");
        UUID ligneMarche = bordereau(lot);
        taux(lot, "0.05", ZERO, ZERO, ZERO, ZERO);
        depassement(decompte(lot, "OVD", 1), ligneMarche);

        resteAPayer("OV", "1368");
    }

    /** R3 PrjWorkingOutTotalProrataRefundAmount : bordereau 1200 + remboursement prorata 30 d'un décompte = 1230. */
    @Test
    void r3_remboursementProrataDesDecomptes_ajoute() throws Exception {
        UUID lot = lot("PR");
        bordereau(lot);
        fixture.decompte(lot, c.code("PRD"), 1, ZERO, ZERO, "30", JOUR);

        resteAPayer("PR", "1230");
    }

    /** R3 PrjWorkingOutRetainedLineAmount : bordereau 1200 − retenue forfaitaire 40 d'un décompte = 1160. */
    @Test
    void r3_retenuesForfaitairesDesDecomptes_deduites() throws Exception {
        UUID lot = lot("RL");
        bordereau(lot);
        fiche.retenueForfaitaire(decompte(lot, "RLD", 1), "40");

        resteAPayer("RL", "1160");
    }

    /**
     * R3 OffredProportionRefund : P = lot prorata du projet (ManageProrataPrjMarketId), sans bordereau → 0 − 0 + Σ des
     * prorata du projet (lot Q : 0,05 × 1200 = 60) = 60 ; Q (autre type) = 1200 − 60 = 1140.
     */
    @Test
    void r3_lotProrataDuProjet_recoitLeProrataDesAutresLots() throws Exception {
        UUID p = lot("PP");
        UUID q = lot("PQ");
        bordereau(q);
        taux(q, "0.05", ZERO, ZERO, ZERO, ZERO);
        maj("UPDATE PrjProject SET ManageProrataPrjMarketId = ? WHERE PKey = ?", p.toString(), c.pa1().toString());

        resteAPayer("PP", "60");
        resteAPayer("PQ", "1140");
    }

    /** R3 et resteTotal : bordereau 1200, facture du lot réglée 300 → payé 300, reste 900 ; pas d'estimation → resteTotal 900. */
    @Test
    void r3_resteAPayer_dueMoinsPaye_resteTotal_sansEstimation() throws Exception {
        UUID lot = lot("RG");
        bordereau(lot);
        UUID piece = s.piece(lot, c.pa1(), "RGF", "500", "500", true);
        s.regler(piece, "RGR", "300");

        Map<String, Object> l = ligne("RG");

        SoftAssertions sa = new SoftAssertions();
        egal(sa, l, PAYE, "300");
        egal(sa, l, RESTE_A_PAYER, "900");
        egal(sa, l, RESTE_MARCHES, "900");
        egal(sa, l, RESTE_ESTIMATIONS, ZERO);
        egal(sa, l, RESTE_TOTAL, "900");
        sa.assertAll();
    }

    // ------------------------------------------------------------------ R4 WarrantyAmount

    /** R4 : R.G. 10 % du devis retenu 2400 (marché d'origine) = 240 + R.G. stockée de l'avenant 12 = 252 ; bordereau ignoré. */
    @Test
    void r4_retenueGarantie_marcheOrigineEtAvenants() throws Exception {
        UUID lot = lot("WA");
        bordereau(lot);
        fixture.marche(lot, "WarrantyRatio", new BigDecimal("0.1"));
        s.marche(lot, c.pa1(), "WAD", "2400");
        UUID avenant = fiche.avenant(lot, c.code("WAV"), CENT, "20", "120");
        maj("""
                IF COL_LENGTH('dbo.PrjClause', 'WarrantyAmount') IS NULL
                    ALTER TABLE dbo.PrjClause ADD WarrantyAmount decimal(26, 6) NULL;""");
        maj("UPDATE PrjClause SET WarrantyAmount = 12 WHERE PKey = ?", avenant.toString());

        Map<String, Object> l = ligne("WA");

        assertThat(montant(l, RETENUE_GARANTIE)).isCloseTo(new BigDecimal("252"), within(PRECISION));
    }

    /**
     * R4 : décompte facturé portant un dépassement 240 et un extra 120 (retenues sur travaux sup. appliquées), R.G. 10 %
     * → 24 + 12 = 36 ; l'extra 1200 d'un décompte non facturé n'entre pas ; pas de devis.
     */
    @Test
    void r4_retenueGarantie_depassementsEtExtrasFactures() throws Exception {
        UUID lot = lot("WB");
        UUID ligneMarche = bordereau(lot);
        fixture.marche(lot, "WarrantyRatio", new BigDecimal("0.1"));
        fixture.marche(lot, "ApplyWithholdingTaxOnAdditionalWork", true);
        UUID facture = decompte(lot, "WBD", 1);
        depassement(facture, ligneMarche);
        decomptes.extra(facture, UN, CENT, TVA);
        facturer(lot, facture, "WBF");
        decomptes.extra(decompte(lot, "WBN", 2), UN, MILLE, TVA);

        Map<String, Object> l = ligne("WB");

        assertThat(montant(l, RETENUE_GARANTIE)).isCloseTo(new BigDecimal("36"), within(PRECISION));
    }

    // ------------------------------------------------------------------ R5 EstimationAmountRest

    /**
     * R5 : estimation 1000 HT = 1200 TTC − (prorata 0,02 × 1200 + DEC montant 10 prioritaire + TRC 0,01 × 1200) = 1154 sans
     * fournisseur ; le même lot avec fournisseur → 0. resteTotal = resteMarches (0, pas de bordereau) + resteEstimations.
     */
    @Test
    void r5_resteEstimations_netSansFournisseur_zeroAvecFournisseur() throws Exception {
        UUID sans = lot("ES");
        UUID avec = lot("EF");
        for (UUID lot : List.of(sans, avec)) {
            s.estimationBase(lot, MILLE);
            taux(lot, DEUX_POUR_CENT, "0.5", DIX, UN_POUR_CENT, ZERO);
        }
        maj("UPDATE PrjMarket SET CfgSupplierId = NULL WHERE PKey = ?", sans.toString());
        fixture.fournisseur(avec, c.fournisseur());

        Map<String, Object> ls = ligne("ES");
        Map<String, Object> lf = ligne("EF");

        SoftAssertions sa = new SoftAssertions();
        egal(sa, ls, RESTE_ESTIMATIONS, "1154");
        egal(sa, ls, RESTE_TOTAL, "1154");
        egal(sa, lf, RESTE_ESTIMATIONS, ZERO);
        egal(sa, lf, RESTE_TOTAL, ZERO);
        sa.assertAll();
    }

    /**
     * R5 × CancelRatio : lot résilié, bordereau 1000 HT, décompte 500 HT → AchievementPrev 0,5 ; estimation 1200 TTC sans
     * retenue → 600.
     */
    @Test
    void r5_resteEstimations_lotResilie_foisCancelRatio() throws Exception {
        UUID lot = s.lotResilie(c.pa1(), "ER");
        typer(lot, type("ER"), s.categorie("CER", null, 1));
        taux(lot, ZERO, ZERO, ZERO, ZERO, ZERO);
        maj("UPDATE PrjMarket SET CfgSupplierId = NULL WHERE PKey = ?", lot.toString());
        bordereau(lot);
        bilans.decompte(lot, "ERD", "500");
        s.estimationBase(lot, MILLE);

        Map<String, Object> l = ligne("ER");

        assertThat(montant(l, RESTE_ESTIMATIONS)).isCloseTo(new BigDecimal("600"), within(PRECISION));
    }

    // ------------------------------------------------------------------ ratioSupplementaires

    /**
     * Groupe A + B (même type, même catégorie) : extras facturés 120 (A) et 0 (B), OfferedAmountToPay 1200 (A) et 2400 (B)
     * → Σ 120 / Σ 3600 = 0,0333 (pas la moyenne 0,05). Lot Z (autre type), extra facturé 120 sans bordereau → 0.
     */
    @Test
    void ratioSupplementaires_sommeSurSomme_zeroSiDenominateurNul() throws Exception {
        UUID a = lot("RA");
        UUID b = s.lot(c.pa1(), "RB");
        maj("UPDATE PrjMarket SET PrjMarketTypeId = (SELECT PrjMarketTypeId FROM PrjMarket WHERE PKey = ?), "
                + "StlCategoryId = (SELECT StlCategoryId FROM PrjMarket WHERE PKey = ?) WHERE PKey = ?",
                a.toString(), a.toString(), b.toString());
        taux(b, ZERO, ZERO, ZERO, ZERO, ZERO);
        bordereau(a);
        decomptes.ligneMarche(b, 1, DIX, "200", TVA);
        UUID da = decompte(a, "RAD", 1);
        decomptes.extra(da, UN, CENT, TVA);
        facturer(a, da, "RAF");
        UUID z = lot("RZ");
        UUID dz = decompte(z, "RZD", 1);
        decomptes.extra(dz, UN, CENT, TVA);
        facturer(z, dz, "RZF");

        Map<String, Object> groupe = ligne("RA");
        Map<String, Object> sansMarche = ligne("RZ");

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(montant(groupe, RATIO)).as("groupe").isCloseTo(new BigDecimal("0.033333"), within(PRECISION_RATIO));
        sa.assertThat(montant(sansMarche, RATIO)).as("dénominateur nul").isCloseTo(BigDecimal.ZERO, within(PRECISION_RATIO));
        sa.assertAll();
    }
}
