package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tranche en écriture — FEATURE-patrimoine-ecriture.md §2, §4, §11 (TrancheEcritureTest). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheEcritureTest {

    private static final String TRANCHES = "/api/tranches";
    private static final String TRANCHE = "/api/tranches/{id}";
    private static final String PROJET_ID = "projetId";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String ADVANCE_PERCENT = "advancePercent";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private CfgTrancheRepository trancheRepository;

    private AdmUser admin;
    private CfgCompany entite;
    private CfgProject projet;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        entite = fixture.entite("ENT-TR-ECR", true);
        projet = fixture.projet("PRJ-TR-ECR", entite);
    }

    private CorpsJson corpsMinimal(String code) {
        return corps().avec(PROJET_ID, projet.getId()).avec(CODE, code).avec(DESCRIPTION, "Tranche " + code);
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TRANCHES).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(TRANCHE, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        String json = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    // ------------------------------------------------------------------ §4.4 valeurs par défaut

    @Test
    void r4_4_creation_booleensParDefautOmran() throws Exception {
        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("TR-ECR-DEF-BOOL")));

        assertAll(
                () -> assertThat(t.get("IsForSale")).as("IsForSale").isEqualTo(true),
                () -> assertThat(t.get("IsForRent")).as("IsForRent").isEqualTo(false),
                () -> assertThat(t.get("IsForProject")).as("IsForProject").isEqualTo(false),
                () -> assertThat(t.get("IsForBuy")).as("IsForBuy").isEqualTo(false),
                () -> assertThat(t.get("IsForSav")).as("IsForSav").isEqualTo(false),
                () -> assertThat(t.get("IsForPrm")).as("IsForPrm").isEqualTo(false),
                () -> assertThat(t.get("IsForRom")).as("IsForRom").isEqualTo(false),
                () -> assertThat(t.get("IsWithGiftForProposer")).as("IsWithGiftForProposer").isEqualTo(false),
                () -> assertThat(t.get("IsWithGiftForOldCustomer")).as("IsWithGiftForOldCustomer").isEqualTo(false),
                () -> assertThat(t.get("IsWithGiftForProspectionOrigin")).as("IsWithGiftForProspectionOrigin")
                        .isEqualTo(false),
                () -> assertThat(t.get("AccumulatedGifts")).as("AccumulatedGifts").isEqualTo(false),
                () -> assertThat(t.get("IsWithVerifCredit")).as("IsWithVerifCredit").isEqualTo(false),
                () -> assertThat(t.get("IsWithVerifMaxEcheanceDelai")).as("IsWithVerifMaxEcheanceDelai")
                        .isEqualTo(false),
                () -> assertThat(t.get("AdvanceBasedOnAmount")).as("AdvanceBasedOnAmount").isEqualTo(false),
                () -> assertThat(t.get("IsActiveAdvancePercentDiscount")).as("IsActiveAdvancePercentDiscount")
                        .isEqualTo(false),
                () -> assertThat(t.get("CancellationPenalityBasedOnItemPrice"))
                        .as("CancellationPenalityBasedOnItemPrice").isEqualTo(true));
    }

    @Test
    void r4_4_creation_delaisEtEcartsTypesParDefautOmran() throws Exception {
        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("TR-ECR-DEF-DELAIS")));

        assertAll(
                () -> assertThat(t.get("AgreementFirstDelay")).as("AgreementFirstDelay").isEqualTo(60),
                () -> assertThat(t.get("AgreementSecondDelay")).as("AgreementSecondDelay").isEqualTo(30),
                () -> assertThat(t.get("WithdrawalDelay")).as("WithdrawalDelay").isEqualTo(48),
                () -> assertThat(t.get("InterventionMinimumDelai")).as("InterventionMinimumDelai").isEqualTo(2),
                () -> assertThat(t.get("MaxVisitNumber")).as("MaxVisitNumber").isEqualTo(4),
                () -> assertThat(t.get("DelayLawyerMinute")).as("DelayLawyerMinute").isEqualTo(60),
                () -> assertThat(t.get("DelayMinuteAllSettled")).as("DelayMinuteAllSettled").isEqualTo(30),
                () -> assertThat(decimal(t.get("EcarttypeConcretisation"))).as("EcarttypeConcretisation")
                        .isEqualByComparingTo("5"),
                () -> assertThat(decimal(t.get("EcarttypeFinalisation"))).as("EcarttypeFinalisation")
                        .isEqualByComparingTo("3"));
    }

    /** §4.4 « tout autre numérique = 0 » : chaque colonne decimal/int sans défaut propre. */
    @Test
    void r4_4_creation_autresNumeriquesAZero() throws Exception {
        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("TR-ECR-DEF-ZERO")));

        assertThat(List.of("AdvancePercent", "ConcretisationPercent", "BeforeReservationAmount", "SyndicAmount",
                "SyndicPeriod", "WarrantyDelayAdded", "WarrantyDelay", "PaymentBalanceDelay",
                "BeforeReservationAmountDiscount", "AdvancePercentDiscount", "DefaultExpenseAmountDiscount",
                "DefaultExpenseAmount", "CancellationPenalityPercent", "CancellationPenalityPercentDiscount",
                "ArticleTransferPenality", "ArticleTransferPenalityDiscount", "SliceTransferPenality",
                "SliceTransferPenalityDiscount", "CustomerTransferPenality", "CustomerTransferPenalityDiscount",
                "SyndicAmountDiscount", "OtherExpenseAmount", "OtherExpenseAmountDiscount",
                "AgreementFirstPenaltyPercent", "AgreementSecondPenaltyPercent", "CountItemBasePrevision",
                "TotalSaleBasePrevision", "BaseEcarttypeConcretisation", "BaseEcarttypeFinalisation",
                "BaseConcretisationPercent", "UnitConcretisation", "UnitFinalisation", "UnitTurnover",
                "UnitTurnoverHT", "CoveredFloorArea", "AdvanceAmount", "MaxEcheanceDelai", "CreditPercent"))
                .isNotEmpty()
                .allSatisfy(colonne -> assertThat(decimal(t.get(colonne))).as(colonne).isEqualByComparingTo("0"));
    }

    @Test
    void r4_4_creation_textesVides() throws Exception {
        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("TR-ECR-DEF-TXT")));

        assertThat(List.of("Address", "Email", "Mobile", "Phone", "Fax", "WebSite", "Address2", "Address3",
                "ZipCode", "City", "CommerceTribunal", "CancellationRightCode", "DescriptionLong", "SavAddress"))
                .isNotEmpty()
                .allSatisfy(colonne -> assertThat(t.get(colonne)).as(colonne).isEqualTo(""));
    }

    @Test
    void r4_4_creation_datesEtClesEtrangeresHorsLotNulles() throws Exception {
        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("TR-ECR-DEF-NULL")));

        assertThat(List.of("WorkStartDate", "WorkEndDate", "MarketingStartDate", "MarketingEndDate", "DeliveryDate",
                "OccupancyPermitsDate", "ReceptionDate", "Revival1Date", "EndDateConcretisation",
                "StartDateFinalisation", "AdmCountryId", "StkLevelingId", "CfgClaimsManagerId", "CfgTierOtherId",
                "CfgTrancheStandingId", "LndLevelId", "ActAccountId", "PrjProjectId", "StkTrancheHeadingId"))
                .isNotEmpty()
                .allSatisfy(colonne -> assertThat(t.get(colonne)).as(colonne).isNull());
    }

    @Test
    void r4_2_creation_estEnVenteParDefautTrue_dansLaReponse() throws Exception {
        creer(corpsMinimal("TR-ECR-VENTE")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estEnVente").value(true))
                .andExpect(jsonPath("$.estEnLocation").value(false));
    }

    @Test
    void r4_4_creation_leCorpsSurchargeLesDefauts() throws Exception {
        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("TR-ECR-SURCHARGE")
                .avec("estEnVente", false).avec("estEnLocation", true)
                .avec("delaiPremierEngagement", 90).avec("maxVisitNumber", 6)
                .avec("penaliteAnnulationSurPrixArticle", false)));

        assertAll(
                () -> assertThat(t.get("IsForSale")).isEqualTo(false),
                () -> assertThat(t.get("IsForRent")).isEqualTo(true),
                () -> assertThat(t.get("AgreementFirstDelay")).isEqualTo(90),
                () -> assertThat(t.get("MaxVisitNumber")).isEqualTo(6),
                () -> assertThat(t.get("CancellationPenalityBasedOnItemPrice")).isEqualTo(false),
                () -> assertThat(t.get("AgreementSecondDelay")).as("défaut des champs absents").isEqualTo(30));
    }

    @Test
    void r4_2_creation_champsEcritsDansLeursColonnes() throws Exception {
        AdmCountry pays = fixture.pays("PAYS-TR-ECR");

        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("TR-ECR-COLONNES")
                .avec("paysId", pays.getId())
                .avec("dateOctroiTitresFonciers", "2027-03-01T00:00:00")
                .avec("datePermisHabiter", "2027-02-01T00:00:00")
                .avec("datePremiereRelance", "2026-12-01T00:00:00")
                .avec("montantPreReservation", new BigDecimal("5000"))
                .avec("avanceSurMontant", true)
                .avec("montantAvance", new BigDecimal("20000"))
                .avec("advancePercentRemise", new BigDecimal("0.1"))
                .avec("fraisDossier", new BigDecimal("1500"))
                .avec("autresFrais", new BigDecimal("300"))
                .avec("periodeSyndic", 24)
                .avec("codeDroitRetractation", "DR-01")
                .avec("tribunalCommerce", "Casablanca")
                .avec("delaiRetractation", 72)
                .avec("verifierDelaiMaxEcheance", true)
                .avec("delaiNotaireMinutes", 45)
                .avec("delaiToutSoldeMinutes", 20)
                .avec("ecartTypeConcretisation", new BigDecimal("7"))
                .avec("ecartTypeFinalisation", new BigDecimal("4"))));

        assertAll(
                () -> assertThat(guid(t.get("AdmCountryId"))).isEqualTo(guid(pays.getId())),
                () -> assertThat(t.get("DeliveryDate")).isNotNull(),
                () -> assertThat(t.get("OccupancyPermitsDate")).isNotNull(),
                () -> assertThat(t.get("Revival1Date")).isNotNull(),
                () -> assertThat(decimal(t.get("BeforeReservationAmount"))).isEqualByComparingTo("5000"),
                () -> assertThat(t.get("AdvanceBasedOnAmount")).isEqualTo(true),
                () -> assertThat(decimal(t.get("AdvanceAmount"))).isEqualByComparingTo("20000"),
                () -> assertThat(decimal(t.get("AdvancePercentDiscount"))).isEqualByComparingTo("0.1"),
                () -> assertThat(decimal(t.get("DefaultExpenseAmount"))).isEqualByComparingTo("1500"),
                () -> assertThat(decimal(t.get("OtherExpenseAmount"))).isEqualByComparingTo("300"),
                () -> assertThat(t.get("SyndicPeriod")).isEqualTo(24),
                () -> assertThat(t.get("CancellationRightCode")).isEqualTo("DR-01"),
                () -> assertThat(t.get("CommerceTribunal")).isEqualTo("Casablanca"),
                () -> assertThat(t.get("WithdrawalDelay")).isEqualTo(72),
                () -> assertThat(t.get("IsWithVerifMaxEcheanceDelai")).isEqualTo(true),
                () -> assertThat(t.get("DelayLawyerMinute")).isEqualTo(45),
                () -> assertThat(t.get("DelayMinuteAllSettled")).isEqualTo(20),
                () -> assertThat(decimal(t.get("EcarttypeConcretisation"))).isEqualByComparingTo("7"),
                () -> assertThat(decimal(t.get("EcarttypeFinalisation"))).isEqualByComparingTo("4"));
    }

    // ------------------------------------------------------------------ §4.4 ConcretisationPercent

    @Test
    void r4_4_creation_concretisationEgaleAdvancePercent() throws Exception {
        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("TR-ECR-CONCR")
                .avec(ADVANCE_PERCENT, new BigDecimal("0.3"))));

        assertAll(
                () -> assertThat(decimal(t.get("AdvancePercent"))).isEqualByComparingTo("0.3"),
                () -> assertThat(decimal(t.get("ConcretisationPercent"))).isEqualByComparingTo("0.3"));
    }

    @Test
    void r4_4_patch_concretisationSuitAdvancePercent() throws Exception {
        UUID id = creerOk(corpsMinimal("TR-ECR-CONCR-PATCH").avec(ADVANCE_PERCENT, new BigDecimal("0.3")));

        modifier(id, corps().avec(ADVANCE_PERCENT, new BigDecimal("0.4"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.concretisationPercent").value(0.4));

        assertThat(decimal(fixture.ligneTranche(id).get("ConcretisationPercent"))).isEqualByComparingTo("0.4");
    }

    // ------------------------------------------------------------------ §2.3 / §2.7

    @Test
    void r2_3_creation_trace() throws Exception {
        Tuple t = fixture.ligneTranche(creerOk(corpsMinimal("TR-ECR-TRACE")));

        assertAll(
                () -> assertThat(t.get("UserCreate")).isEqualTo(AUTEUR),
                () -> assertThat(t.get("UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(t.get("UserCreateDateTime")).isNotNull(),
                () -> assertThat(t.get("UserUpdateDateTime")).isEqualTo(t.get("UserCreateDateTime")),
                () -> assertThat(guid(t.get("CfgProjectId"))).isEqualTo(guid(projet.getId())));
    }

    @Test
    void r2_7_creation_habilitationDuCreateurSurLaTrancheSeule() throws Exception {
        UUID id = creerOk(corpsMinimal("TR-ECR-HABIL"));

        assertThat(fixture.habilitations(admin.getId())).singleElement().satisfies(h -> assertAll(
                () -> assertThat(guid(h.get("CfgTrancheId"))).isEqualTo(guid(id)),
                () -> assertThat(h.get("CfgProjectId")).isNull(),
                () -> assertThat(h.get("CfgCompanyId")).isNull(),
                () -> assertThat(h.get("PrjProjectId")).isNull()));
    }

    @Test
    void r4_3_creation_reponseExposeProjetEntiteEtTrace() throws Exception {
        creer(corpsMinimal("TR-ECR-REPONSE").avec("dateOctroiTitresFonciers", "2027-03-01T00:00:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projetId").value(projet.getId().toString()))
                .andExpect(jsonPath("$.projetCode").value("PRJ-TR-ECR"))
                .andExpect(jsonPath("$.entiteId").value(entite.getId().toString()))
                .andExpect(jsonPath("$.entiteCode").value("ENT-TR-ECR"))
                .andExpect(jsonPath("$.creePar").value(AUTEUR))
                .andExpect(jsonPath("$.modifiePar").value(AUTEUR))
                .andExpect(jsonPath("$.creeLe").isNotEmpty())
                .andExpect(jsonPath("$.dateOctroiTitresFonciers").isNotEmpty())
                .andExpect(jsonPath("$.dateLivraison").doesNotExist());
    }

    // ------------------------------------------------------------------ §4.4 / §2.8 refus 400

    @Test
    void r4_4_creditPercentSuperieurA1_400() throws Exception {
        creer(corpsMinimal("TR-ECR-CREDIT").avec("creditPercent", new BigDecimal("1.5")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"tauxPenaliteAnnulation", "tauxPenaliteAnnulationRemise", "tauxPenalitePremierEngagement",
            "tauxPenaliteDeuxiemeEngagement", ADVANCE_PERCENT, "advancePercentRemise", "creditPercent"})
    void r4_4_fractionNegative_400(String champ) throws Exception {
        creer(corpsMinimal("TR-ECR-NEG-" + champ).avec(champ, new BigDecimal("-0.1")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"tauxPenaliteAnnulation", ADVANCE_PERCENT, "advancePercentRemise"})
    void r4_4_fractionSuperieureA1_400(String champ) throws Exception {
        creer(corpsMinimal("TR-ECR-SUP-" + champ).avec(champ, new BigDecimal("1.5")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"montantPreReservation", "montantAvance", "fraisDossier", "autresFrais", "montantSyndic"})
    void r4_4_montantNegatif_400(String champ) throws Exception {
        creer(corpsMinimal("TR-ECR-MNT-" + champ).avec(champ, new BigDecimal("-1")))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"delaiPremierEngagement", "delaiDeuxiemeEngagement", "delaiSoldePaiement",
            "delaiGarantie", "delaiRetractation", "delaiMaxEcheance", "periodeSyndic"})
    void r4_4_delaiNegatif_400(String champ) throws Exception {
        creer(corpsMinimal("TR-ECR-DEL-" + champ).avec(champ, -1)).andExpect(status().isBadRequest());
    }

    @Test
    void r4_2_projetManquant_400() throws Exception {
        creer(corpsMinimal("TR-ECR-SANS-PRJ").sans(PROJET_ID)).andExpect(status().isBadRequest());
    }

    @Test
    void r2_8_projetInconnu_400() throws Exception {
        creer(corpsMinimal("TR-ECR-PRJ-INC").avec(PROJET_ID, UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    @Test
    void r4_2_codeManquant_400() throws Exception {
        creer(corpsMinimal("X").sans(CODE)).andExpect(status().isBadRequest());
    }

    @Test
    void r4_2_descriptionManquante_400() throws Exception {
        creer(corpsMinimal("TR-ECR-SANS-DESC").sans(DESCRIPTION)).andExpect(status().isBadRequest());
    }

    @Test
    void r4_4_finTravauxAvantDebut_400() throws Exception {
        creer(corpsMinimal("TR-ECR-DATES-TRV")
                .avec("dateDebutTravaux", "2026-06-01T00:00:00").avec("dateFinTravaux", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r4_4_finCommercialisationAvantDebut_400() throws Exception {
        creer(corpsMinimal("TR-ECR-DATES-COM")
                .avec("dateDebutCommercialisation", "2026-06-01T00:00:00")
                .avec("dateFinCommercialisation", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    /** §14.5 : la règle fin ≥ début porte sur la valeur résultante (corps, sinon base). */
    @Test
    void r14_5_patch_finTravauxAvantLeDebutEnBase_400() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-ECR-PATCH-DATES", projet);
        tranche.setWorkStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        trancheRepository.save(tranche);

        modifier(tranche.getId(), corps().avec("dateFinTravaux", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r14_5_patch_debutCommercialisationApresLaFinEnBase_400() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-ECR-PATCH-DATES-COM", projet);
        tranche.setMarketingEndDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        trancheRepository.save(tranche);

        modifier(tranche.getId(), corps().avec("dateDebutCommercialisation", "2026-06-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r14_5_patch_finApresLeDebutEnBase_200() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-ECR-PATCH-DATES-OK", projet);
        tranche.setWorkStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        trancheRepository.save(tranche);

        modifier(tranche.getId(), corps().avec("dateFinTravaux", "2027-01-01T00:00:00")).andExpect(status().isOk());
    }

    @Test
    void r4_2_patch_changementDeProgramme_400() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-ECR-CHG-PRJ", projet);
        CfgProject autre = fixture.projet("PRJ-TR-ECR-AUTRE", entite);

        modifier(tranche.getId(), corps().avec(PROJET_ID, autre.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Le programme d'une tranche ne se modifie pas")));
    }

    // ------------------------------------------------------------------ §2.6 unicité globale 409

    @Test
    void r2_6_codeDejaPrisDansUnAutreProgramme_409() throws Exception {
        CfgProject autre = fixture.projet("PRJ-TR-ECR-AUTRE-2", entite);
        CfgTranche existante = fixture.tranche("TR-ECR-GLOBAL", autre);

        creer(corpsMinimal("tr-ecr-global"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(existante.getCode())))
                .andExpect(jsonPath("$.detail").value(containsString(existante.getDescription())));
    }

    @Test
    void r2_6_patch_codeDUneAutreTranche_409() throws Exception {
        fixture.tranche("TR-ECR-PRIS", projet);
        CfgTranche cible = fixture.tranche("TR-ECR-CIBLE", projet);

        modifier(cible.getId(), corps().avec(CODE, "TR-ECR-PRIS")).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ §4.1 / §2.9 modification

    @Test
    void r4_1_patch_idInconnu_404() throws Exception {
        modifier(UUID.randomUUID(), corps().avec(DESCRIPTION, "x")).andExpect(status().isNotFound());
    }

    @Test
    void r2_9_patch_champAbsentInchange_chaineVideVidee_trace() throws Exception {
        CfgTranche cible = fixture.tranche("TR-ECR-PATCH", projet);
        cible.setAddress("5 avenue C");
        cible.setCity("Tanger");
        cible.setUserCreate("Auteur OMRAN");
        trancheRepository.save(cible);

        modifier(cible.getId(), corps().avec("ville", "")).andExpect(status().isOk());

        Tuple t = fixture.ligneTranche(cible.getId());
        assertAll(
                () -> assertThat(t.get("City")).isEqualTo(""),
                () -> assertThat(t.get("Address")).isEqualTo("5 avenue C"),
                () -> assertThat(t.get("Description")).isEqualTo("Tranche TR-ECR-PATCH"),
                () -> assertThat(t.get("UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(t.get("UserUpdateDateTime")).isNotNull(),
                () -> assertThat(t.get("UserCreate")).isEqualTo("Auteur OMRAN"));
    }

    // ------------------------------------------------------------------ §4.1 listes

    @Test
    void r4_1_listeDesTranches_exposeProjetIdEtProjetCode() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-ECR-LISTE", projet);
        tranche.setDeliveryDate(LocalDateTime.of(2027, 3, 1, 0, 0));
        trancheRepository.save(tranche);
        String filtre = "$.content[?(@.id=='" + tranche.getId() + "')]";

        mockMvc.perform(get(TRANCHES).param(PROJET_ID, projet.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre).exists())
                .andExpect(jsonPath(filtre + ".projetId").value(projet.getId().toString()))
                .andExpect(jsonPath(filtre + ".projetCode").value("PRJ-TR-ECR"))
                .andExpect(jsonPath(filtre + ".dateOctroiTitresFonciers").value("2027-03-01T00:00:00"))
                .andExpect(jsonPath(filtre + ".dateLivraison").doesNotExist());
    }

    @Test
    void r4_1_tranchesDuProjet_exposeProjetId() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-ECR-LISTE-PRJ", projet);
        String filtre = "$.content[?(@.id=='" + tranche.getId() + "')]";

        mockMvc.perform(get("/api/projets/{id}/tranches", projet.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre).exists())
                .andExpect(jsonPath(filtre + ".projetId").value(projet.getId().toString()));
    }

    @Test
    void r4_3_detail_dateLivraisonRenommeeDateOctroiTitresFonciers() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-ECR-OCTROI", projet);
        tranche.setDeliveryDate(LocalDateTime.of(2027, 3, 1, 0, 0));
        trancheRepository.save(tranche);

        mockMvc.perform(get(TRANCHE, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateOctroiTitresFonciers").value("2027-03-01T00:00:00"))
                .andExpect(jsonPath("$.dateLivraison").doesNotExist());
    }
}
