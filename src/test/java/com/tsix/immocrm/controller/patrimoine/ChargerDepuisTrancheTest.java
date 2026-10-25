package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureChargementTranche;
import com.tsix.immocrm.fixtures.FixtureChargementTranche.LigneFrais;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.fixtures.FixtureTrancheOnglets;
import com.tsix.immocrm.fixtures.FixtureTrancheRegles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureChargementTranche.ligne;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Charger à partir d'une autre tranche » — FEATURE-patrimoine-reliquat-v2.md §A.2, §A.3, §A.6 règles 1 à 6 et 9 (trace
 * {@code UserUpdate}). Données en SQL natif : le test ne dépend pas des classes que le lot crée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChargerDepuisTrancheTest {

    static final String CHARGER = "/api/tranches/{id}/charger-depuis/{sourceId}";

    private static final String ETAGES =
            "SELECT CONVERT(varchar(36), LndLevelId) AS k FROM CfgTrancheLevel WHERE CfgTrancheId = :id";
    private static final String PLANCHERS = """
            SELECT CONVERT(varchar(36), StkItemCategoryId) AS k, Area AS a
            FROM CfgTrancheCoveredFloor WHERE CfgTrancheId = :id""";
    private static final String FRAIS = """
            SELECT CONVERT(varchar(36), ComSettlementCategoryId) AS k, Sorting AS s, AmountHT AS ht, VatRatio AS tva,
                   PercentOf AS p, Notes AS n
            FROM CfgTrancheSettlementCategory WHERE CfgtrancheId = :id""";
    private static final String FINALISATIONS = """
            SELECT CONVERT(varchar(36), StrFolderEntityId) AS k, CONVERT(varchar(36), StrStatusDeliveryId1) AS s1,
                   CONVERT(varchar(36), StrStatusDeliveryId2) AS s2
            FROM CfgTrancheFolderEntity WHERE CfgTrancheId = :id""";
    private static final String CONCRETISATION = "ConcretisationPercent";
    private static final String ADRESSE = "Address";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTrancheFiche fiche;
    @Autowired private FixtureTrancheOnglets onglets;
    @Autowired private FixtureTrancheRegles regles;
    @Autowired private FixtureReferentielsAdmin referentiels;
    @Autowired private FixtureChargementTranche chargement;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private CfgTranche source;
    private CfgTranche cible;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = "P5B" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entite = patrimoine.entite("ENT-" + jeton, true);
        source = patrimoine.tranche("TR-S-" + jeton, patrimoine.projet("PRJ-S-" + jeton, entite));
        cible = patrimoine.tranche("TR-C-" + jeton, patrimoine.projet("PRJ-C-" + jeton, entite));
    }

    private ResultActions charger() throws Exception {
        return mockMvc.perform(post(CHARGER, cible.getId(), source.getId()).with(connecte(admin)));
    }

    private String code(String prefixe) {
        return prefixe + "-" + jeton;
    }

    // ------------------------------------------------------------------ jeux de valeurs

    /** Champs de §A.2, tous non nuls sur la source (la cible part de NULL partout). */
    private Map<String, Object> parametresCopies() {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("CfgTrancheStandingId", fiche.standing(code("STD")));
        v.put("LndLevelId", fiche.niveauTerrain(code("NIVT")));
        v.put("CfgClaimsManagerId", fiche.employe(code("SAV")).getId());
        v.put(ADRESSE, "1 rue Source");
        v.put("Address2", "Bloc S");
        v.put("Address3", "Etage S");
        v.put("ZipCode", "20000");
        v.put("City", "Casablanca");
        v.put("AdmCountryId", patrimoine.pays(code("PAY")).getId());
        v.put("Phone", "0522000001");
        v.put("Fax", "0522000002");
        v.put("Mobile", "0661000003");
        v.put("Email", "source@p5b.ma");
        v.put("WebSite", "www.source-p5b.ma");
        v.put("SavAddress", "SAV rue Source");
        v.put("BeforeReservationAmount", new BigDecimal("5000"));
        v.put("AdvanceBasedOnAmount", true);
        v.put("AdvancePercent", new BigDecimal("0.35"));
        v.put("AdvanceAmount", new BigDecimal("150000"));
        v.put("DefaultExpenseAmount", new BigDecimal("2500"));
        v.put("CancellationPenalityPercent", new BigDecimal("0.1"));
        v.put("CancellationPenalityBasedOnItemPrice", true);
        v.put("ArticleTransferPenality", new BigDecimal("3000"));
        v.put("SliceTransferPenality", new BigDecimal("4000"));
        v.put("CustomerTransferPenality", new BigDecimal("5000"));
        v.put("SyndicAmount", new BigDecimal("700"));
        v.put("SyndicPeriod", 24);
        v.put("OtherExpenseAmount", new BigDecimal("1200"));
        v.put("AgreementFirstDelay", 30);
        v.put("AgreementSecondDelay", 60);
        v.put("AgreementFirstPenaltyPercent", new BigDecimal("0.02"));
        v.put("AgreementSecondPenaltyPercent", new BigDecimal("0.04"));
        v.put("PaymentBalanceDelay", 90);
        v.put("WarrantyDelay", 12);
        v.put("WarrantyDelayAdded", 6);
        v.put("CommerceTribunal", "Tribunal Source");
        v.put("CancellationRightCode", "DROIT-S");
        v.put("IsWithGiftForProposer", true);
        v.put("IsWithGiftForOldCustomer", true);
        v.put("IsWithGiftForProspectionOrigin", true);
        v.put("AccumulatedGifts", true);
        v.put("BeforeReservationAmountDiscount", new BigDecimal("1000"));
        v.put("AdvancePercentDiscount", new BigDecimal("0.05"));
        v.put("IsActiveAdvancePercentDiscount", true);
        v.put("DefaultExpenseAmountDiscount", new BigDecimal("500"));
        v.put("CancellationPenalityPercentDiscount", new BigDecimal("0.01"));
        v.put("CustomerTransferPenalityDiscount", new BigDecimal("100"));
        v.put("ArticleTransferPenalityDiscount", new BigDecimal("200"));
        v.put("SliceTransferPenalityDiscount", new BigDecimal("300"));
        v.put("SyndicAmountDiscount", new BigDecimal("50"));
        v.put("OtherExpenseAmountDiscount", new BigDecimal("60"));
        v.put("InterventionMinimumDelai", 2);
        v.put("MaxVisitNumber", 5);
        v.put("DelayLawyerMinute", 60);
        v.put("DelayMinuteAllSettled", 30);
        v.put("DescriptionLong", "Description longue source");
        return v;
    }

    /** Champs de §A.3 ; {@code cote} distingue les valeurs de la source et de la cible. Années ≤ 2078 (smalldatetime CI). */
    private Map<String, Object> parametresJamaisCopies(String cote, int annee, boolean drapeau, int base) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("WorkStartDate", LocalDateTime.of(annee, 1, 15, 0, 0));
        v.put("WorkEndDate", LocalDateTime.of(annee + 1, 1, 15, 0, 0));
        v.put("MarketingStartDate", LocalDateTime.of(annee, 2, 15, 0, 0));
        v.put("DeliveryDate", LocalDateTime.of(annee + 2, 6, 1, 0, 0));
        v.put("PrevisionMarketingStartDate", LocalDateTime.of(annee, 3, 1, 0, 0));
        v.put("BaseMarketingStartDate", LocalDateTime.of(annee, 4, 1, 0, 0));
        v.put("IsForSale", drapeau);
        v.put("IsForRent", drapeau);
        v.put("IsForProject", drapeau);
        v.put("IsForBuy", drapeau);
        v.put("IsForSav", drapeau);
        v.put("IsForPrm", drapeau);
        v.put("IsForRom", drapeau);
        v.put("WithdrawalDelay", base);
        v.put("MaxEcheanceDelai", base + 1);
        v.put("IsWithVerifCredit", drapeau);
        v.put("IsWithVerifMaxEcheanceDelai", drapeau);
        v.put("CreditPercent", new BigDecimal(base).movePointLeft(2));
        v.put("EcarttypeFinalisation", new BigDecimal(base + 2));
        v.put("EcarttypeConcretisation", new BigDecimal(base + 3));
        v.put("UnitTurnover", new BigDecimal(base * 1000));
        v.put("BaseConcretisationPercent", new BigDecimal(base + 4).movePointLeft(2));
        v.put("CfgTierOtherId", fiche.fournisseur(code("FOU-" + cote)).getId());
        v.put("StkTrancheHeadingId", fiche.rubrique(code("RUB-" + cote), base));
        v.put("ActAccountId", fiche.planComptable(code("ACT-" + cote)));
        v.put("PrjProjectId", fiche.projetFacturation(code("PRJF-" + cote), entite.getId()));
        return v;
    }

    private static List<String> colonnes(Map<String, Object> valeurs) {
        return List.copyOf(valeurs.keySet());
    }

    // ------------------------------------------------------------------ §A.4 réponse

    @Test
    void a4_reponse200_ficheDeLaCibleRelue() throws Exception {
        charger().andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cible.getId().toString()));
    }

    // ------------------------------------------------------------------ règle 1 : champs et listes copiés

    @Test
    void r1_cibleVide_tousLesChampsDeA2EgalentLaSource() throws Exception {
        Map<String, Object> valeurs = parametresCopies();
        chargement.definir(source.getId(), valeurs);
        List<String> champs = colonnes(valeurs);
        Map<String, String> attendu = chargement.valeurs(source.getId(), champs);

        charger().andExpect(status().isOk());

        assertThat(chargement.valeurs(cible.getId(), champs)).doesNotContainValue(null).isEqualTo(attendu);
    }

    /** §A.2 « la valeur de la source écrase celle de la cible, y compris null ». */
    @Test
    void r1_champNulDeLaSource_ecraseLaValeurDeLaCible() throws Exception {
        chargement.definir(cible.getId(), Map.of(ADRESSE, "Adresse de la cible", "SyndicAmount", new BigDecimal("900")));

        charger().andExpect(status().isOk());

        assertThat(chargement.valeurs(cible.getId(), List.of(ADRESSE, "SyndicAmount")))
                .containsEntry(ADRESSE, null).containsEntry("SyndicAmount", null);
    }

    @Test
    void r1_cibleVide_listesEgalesALaSource() throws Exception {
        listesDeLaSource();

        charger().andExpect(status().isOk());

        assertAll(
                () -> assertThat(chargement.lignes(ETAGES, cible.getId()))
                        .isNotEmpty().containsExactlyInAnyOrderElementsOf(chargement.lignes(ETAGES, source.getId())),
                () -> assertThat(chargement.lignes(PLANCHERS, cible.getId()))
                        .isNotEmpty().containsExactlyInAnyOrderElementsOf(chargement.lignes(PLANCHERS, source.getId())),
                () -> assertThat(chargement.lignes(FRAIS, cible.getId()))
                        .isNotEmpty().containsExactlyInAnyOrderElementsOf(chargement.lignes(FRAIS, source.getId())),
                () -> assertThat(chargement.lignes(FINALISATIONS, cible.getId())).isNotEmpty()
                        .containsExactlyInAnyOrderElementsOf(chargement.lignes(FINALISATIONS, source.getId())));
    }

    private void listesDeLaSource() {
        onglets.ligneEtage(source.getId(), fiche.niveauTerrain(code("NIV1")));
        onglets.lignePlancher(source.getId(), categoriePlancher("CP1"), new BigDecimal("25"));
        chargement.ligneFrais(source.getId(), categorieFrais("FR1"), new LigneFrais(2, new BigDecimal("1000"),
                new BigDecimal("0.2"), new BigDecimal("1200"), new BigDecimal("0.05"), "Notes source"));
        UUID entiteDossier = regles.entiteDossierVente();
        regles.ligneFinalisation(source.getId(), entiteDossier, statut("S1"), statut("S2"));
    }

    private UUID categoriePlancher(String prefixe) {
        return patrimoine.categorie(code(prefixe), patrimoine.natureBat(), false).getId();
    }

    private UUID categorieFrais(String prefixe) {
        return onglets.categorieReglement(code(prefixe), true, true);
    }

    private UUID statut(String prefixe) {
        return referentiels.statut(FixtureReferentielsAdmin.ENTITE_DOSSIER_VENTE, code(prefixe));
    }

    // ------------------------------------------------------------------ règle 2 : fusion par clé

    /** Une clé commune, une propre à la cible, une propre à la source, pour chacune des 4 listes. */
    private record Fusion(UUID niveauCommun, UUID niveauCible, UUID niveauSource,
                          UUID planCommun, UUID planCible, UUID planSource,
                          UUID fraisCommun, UUID fraisCible, UUID fraisSource,
                          UUID entiteCommune, UUID entiteCible, UUID entiteSource,
                          UUID s1, UUID s2, UUID s3) {
    }

    private Fusion scenarioFusion() {
        referentiels.parametrageSeuils();
        patrimoine.entiteStkItem();
        Fusion f = new Fusion(
                fiche.niveauTerrain(code("NC")), fiche.niveauTerrain(code("NCI")), fiche.niveauTerrain(code("NSO")),
                categoriePlancher("PC"), categoriePlancher("PCI"), categoriePlancher("PSO"),
                categorieFrais("FC"), categorieFrais("FCI"), categorieFrais("FSO"),
                FixtureReferentielsAdmin.ENTITE_DOSSIER_VENTE, FixtureReferentielsAdmin.ENTITE_PRM_FOLDER_SALE,
                FixturePatrimoine.ENTITE_STK_ITEM,
                statut("SA"), statut("SB"), statut("SC"));

        onglets.ligneEtage(cible.getId(), f.niveauCommun());
        onglets.ligneEtage(cible.getId(), f.niveauCible());
        onglets.ligneEtage(source.getId(), f.niveauCommun());
        onglets.ligneEtage(source.getId(), f.niveauSource());

        onglets.lignePlancher(cible.getId(), f.planCommun(), new BigDecimal("10"));
        onglets.lignePlancher(cible.getId(), f.planCible(), new BigDecimal("5"));
        onglets.lignePlancher(source.getId(), f.planCommun(), new BigDecimal("25"));
        onglets.lignePlancher(source.getId(), f.planSource(), new BigDecimal("40"));

        chargement.ligneFrais(cible.getId(), f.fraisCommun(), new LigneFrais(9, BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ONE, new BigDecimal("0.5"), "Notes cible"));
        chargement.ligneFrais(cible.getId(), f.fraisCible(), new LigneFrais(3, BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.TEN, BigDecimal.ZERO, "Propre cible"));
        chargement.ligneFrais(source.getId(), f.fraisCommun(), new LigneFrais(2, new BigDecimal("1000"),
                new BigDecimal("0.2"), new BigDecimal("999"), new BigDecimal("0.05"), "Notes source"));
        chargement.ligneFrais(source.getId(), f.fraisSource(), new LigneFrais(4, new BigDecimal("500"),
                new BigDecimal("0.1"), new BigDecimal("550"), BigDecimal.ZERO, "Ajoutee source"));

        regles.ligneFinalisation(cible.getId(), f.entiteCommune(), f.s1(), f.s1());
        regles.ligneFinalisation(cible.getId(), f.entiteCible(), f.s1(), f.s2());
        regles.ligneFinalisation(source.getId(), f.entiteCommune(), f.s2(), f.s3());
        regles.ligneFinalisation(source.getId(), f.entiteSource(), f.s3(), f.s1());
        return f;
    }

    @Test
    void r2_etages_communeUneFois_propreConservee_sourceAjoutee() throws Exception {
        Fusion f = scenarioFusion();

        charger().andExpect(status().isOk());

        assertThat(chargement.lignes(ETAGES, cible.getId()))
                .containsExactlyInAnyOrder(ligne(f.niveauCommun()), ligne(f.niveauCible()), ligne(f.niveauSource()));
    }

    @Test
    void r2_planchers_communePrendLaSurfaceDeLaSource_propreConservee_sourceAjoutee() throws Exception {
        Fusion f = scenarioFusion();

        charger().andExpect(status().isOk());

        assertThat(chargement.lignes(PLANCHERS, cible.getId())).containsExactlyInAnyOrder(
                ligne(f.planCommun(), new BigDecimal("25")),
                ligne(f.planCible(), new BigDecimal("5")),
                ligne(f.planSource(), new BigDecimal("40")));
    }

    @Test
    void r2_frais_communePrendLesValeursDeLaSource_propreConservee_sourceAjoutee() throws Exception {
        Fusion f = scenarioFusion();

        charger().andExpect(status().isOk());

        assertThat(chargement.lignes(FRAIS, cible.getId())).containsExactlyInAnyOrder(
                ligne(f.fraisCommun(), 2, new BigDecimal("1000"), new BigDecimal("0.2"), new BigDecimal("0.05"),
                        "Notes source"),
                ligne(f.fraisCible(), 3, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, "Propre cible"),
                ligne(f.fraisSource(), 4, new BigDecimal("500"), new BigDecimal("0.1"), BigDecimal.ZERO,
                        "Ajoutee source"));
    }

    @Test
    void r2_finalisations_communePrendLesStatutsDeLaSource_propreConservee_sourceAjoutee() throws Exception {
        Fusion f = scenarioFusion();

        charger().andExpect(status().isOk());

        assertThat(chargement.lignes(FINALISATIONS, cible.getId())).containsExactlyInAnyOrder(
                ligne(f.entiteCommune(), f.s2(), f.s3()),
                ligne(f.entiteCible(), f.s1(), f.s2()),
                ligne(f.entiteSource(), f.s3(), f.s1()));
    }

    // ------------------------------------------------------------------ règle 3 : Σ planchers

    /** Décision manager (Question 2) : recalcul, pas la valeur de la source (123) ni l'ancienne de la cible (999). */
    @Test
    void r3_plancherCouvertDeLaCible_egaleLaSommeDesLignesApresFusion() throws Exception {
        scenarioFusion();
        chargement.definir(cible.getId(), Map.of("CoveredFloorArea", new BigDecimal("999")));
        chargement.definir(source.getId(), Map.of("CoveredFloorArea", new BigDecimal("123")));

        charger().andExpect(status().isOk());

        assertThat(chargement.decimal("SELECT CoveredFloorArea FROM CfgTranche WHERE PKey = :id", cible.getId()))
                .isEqualByComparingTo("70");
    }

    // ------------------------------------------------------------------ règle 4 : TTC recalculé

    @Test
    void r4_fraisCopies_ttcEgaleHtFoisUnPlusTva_pasLeTtcDeLaSource() throws Exception {
        Fusion f = scenarioFusion();

        charger().andExpect(status().isOk());

        assertThat(chargement.lignes("""
                SELECT CONVERT(varchar(36), ComSettlementCategoryId) AS k, AmountTTC AS t
                FROM CfgTrancheSettlementCategory WHERE CfgtrancheId = :id""", cible.getId()))
                .contains(ligne(f.fraisCommun(), new BigDecimal("1200")), ligne(f.fraisSource(), new BigDecimal("550")));
    }

    // ------------------------------------------------------------------ règle 5 : jamais copiés

    @Test
    void r5_codeDescriptionProgrammeDatesDrapeauxEtAutres_inchangesSurLaCible() throws Exception {
        chargement.definir(source.getId(), parametresJamaisCopies("S", 2031, true, 10));
        Map<String, Object> deLaCible = parametresJamaisCopies("C", 2041, false, 20);
        chargement.definir(cible.getId(), deLaCible);
        List<String> champs = new ArrayList<>(colonnes(deLaCible));
        champs.addAll(List.of("Code", "Description", "CfgProjectId"));
        Map<String, String> avant = chargement.valeurs(cible.getId(), champs);

        charger().andExpect(status().isOk());

        assertThat(chargement.valeurs(cible.getId(), champs)).isEqualTo(avant);
    }

    @Test
    void r5_seuilsTerrainsCommissions_jamaisCopies() throws Exception {
        referentiels.parametrageSeuils();
        referentiels.seuilDeTranche(cible.getId(), statut("SEU1"), 1, new BigDecimal("0.1"));
        referentiels.seuilDeTranche(source.getId(), statut("SEU2"), 1, new BigDecimal("0.2"));
        referentiels.seuilDeTranche(source.getId(), statut("SEU3"), 1, new BigDecimal("0.3"));
        onglets.lienTerrain(source.getId(), onglets.terrain(code("TER")), BigDecimal.ONE, 1);
        regles.ligneCommission(source.getId(), LocalDateTime.of(2031, 1, 1, 0, 0), LocalDateTime.of(2031, 12, 31, 0, 0));

        charger().andExpect(status().isOk());

        assertAll(
                () -> assertThat(chargement.compter("SELECT COUNT(*) FROM CfgTrancheStatus WHERE CfgTrancheId = :id",
                        cible.getId())).isEqualTo(1),
                () -> assertThat(chargement.compter("SELECT COUNT(*) FROM CfgTrancheStkItem WHERE CfgTrancheId = :id",
                        cible.getId())).isZero(),
                () -> assertThat(chargement.compter("SELECT COUNT(*) FROM ComSalesCommission WHERE CfgTrancheId = :id",
                        cible.getId())).isZero());
    }

    // ------------------------------------------------------------------ règle 6 : concrétisation dérivée

    @Test
    void r6_concretisationPercentDeLaCible_egaleSonAvancePercentApresCopie() throws Exception {
        chargement.definir(source.getId(), Map.of("AdvancePercent", new BigDecimal("0.35"),
                CONCRETISATION, new BigDecimal("0.9")));
        chargement.definir(cible.getId(), Map.of(CONCRETISATION, new BigDecimal("0.1")));

        charger().andExpect(status().isOk());

        assertThat(chargement.decimal("SELECT " + CONCRETISATION + " FROM CfgTranche WHERE PKey = :id", cible.getId()))
                .isEqualByComparingTo("0.35");
    }

    // ------------------------------------------------------------------ règle 9 : trace de la cible

    @Test
    void r9_cibleTracee_userUpdateEtDate() throws Exception {
        chargement.definir(source.getId(), Map.of(ADRESSE, "1 rue Source"));

        charger().andExpect(status().isOk());

        assertAll(
                () -> assertThat(chargement.texte("SELECT UserUpdate FROM CfgTranche WHERE PKey = :id", cible.getId()))
                        .isEqualTo(FixturePatrimoine.AUTEUR),
                () -> assertThat(chargement.texte("SELECT UserUpdateDateTime FROM CfgTranche WHERE PKey = :id",
                        cible.getId())).isNotNull());
    }
}
