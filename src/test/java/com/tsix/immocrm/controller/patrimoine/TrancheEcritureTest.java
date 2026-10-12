package com.tsix.immocrm.controller.patrimoine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-ecriture.md §4 et §11. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheEcritureTest {

    private static final String PROJET_ID = "projetId";
    private static final String CHAMP = "$.champ";
    private static final String AUTEUR = "Compte ADM-TRA-ECR";
    private static final String CODE_PROGRAMME = "PRJ-TRA-ECR";
    private static final String CODE_MINIMAL = "TRA-ECR-1";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private CfgProject projet;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-TRA-ECR");
        CfgCompany entite = fixture.entite("ENT-TRA-ECR", true);
        projet = fixture.projet(CODE_PROGRAMME, entite);
    }

    private Map<String, Object> corpsMinimal(String code) {
        Map<String, Object> corps = new HashMap<>();
        corps.put(PROJET_ID, projet.getId());
        corps.put("code", code);
        corps.put("description", "Tranche " + code);
        return corps;
    }

    private ResultActions creer(Map<String, Object> corps) throws Exception {
        return mockMvc.perform(post("/api/tranches").with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private ResultActions modifier(UUID id, Map<String, Object> corps) throws Exception {
        return mockMvc.perform(patch("/api/tranches/{id}", id).with(connecte(admin, ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private CfgTranche relire(String reponse) throws Exception {
        JsonNode noeud = JsonMapper.builder().build().readTree(reponse);
        em.flush();
        em.clear();
        return trancheRepository.findById(UUID.fromString(noeud.get("id").asText())).orElseThrow();
    }

    /** Valeurs initiales d'OMRAN, colonne par colonne : une tranche créée avec le seul corps minimal. */
    private CfgTranche creerTrancheMinimale() throws Exception {
        return relire(creer(corpsMinimal(CODE_MINIMAL))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void creation_201_reponsePorteProgrammeEntiteEtAuteur() throws Exception {
        String reponse = creer(corpsMinimal(CODE_MINIMAL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projetCode").value(CODE_PROGRAMME))
                .andExpect(jsonPath("$.entiteCode").value("ENT-TRA-ECR"))
                .andExpect(jsonPath("$.creePar").value(AUTEUR))
                .andReturn().getResponse().getContentAsString();
        CfgTranche t = relire(reponse);

        assertAll("auteur et horodatage",
                () -> assertThat(t.getUserCreate()).isEqualTo(AUTEUR),
                () -> assertThat(t.getUserUpdateDateTime()).isNotNull().isEqualTo(t.getUserCreateDateTime()));
    }

    @Test
    void creation_drapeauxInitiauxOmran() throws Exception {
        CfgTranche t = creerTrancheMinimale();

        assertAll("drapeaux",
                () -> assertThat(t.getIsForSale()).isTrue(),
                () -> assertThat(t.getIsForRent()).isFalse(),
                () -> assertThat(t.getIsForProject()).isFalse(),
                () -> assertThat(t.getIsForBuy()).isFalse(),
                () -> assertThat(t.getIsForSav()).isFalse(),
                () -> assertThat(t.getIsForPrm()).isFalse(),
                () -> assertThat(t.getIsForRom()).isFalse(),
                () -> assertThat(t.getIsWithGiftForProposer()).isFalse(),
                () -> assertThat(t.getIsWithGiftForOldCustomer()).isFalse(),
                () -> assertThat(t.getIsWithGiftForProspectionOrigin()).isFalse(),
                () -> assertThat(t.getAccumulatedGifts()).isFalse(),
                () -> assertThat(t.getIsWithVerifCredit()).isFalse(),
                () -> assertThat(t.getIsWithVerifMaxEcheanceDelai()).isFalse(),
                () -> assertThat(t.getAdvanceBasedOnAmount()).isFalse(),
                () -> assertThat(t.getIsActiveAdvancePercentDiscount()).isFalse(),
                () -> assertThat(t.getCancellationPenalityBasedOnItemPrice()).isTrue());
    }

    @Test
    void creation_delaisInitiauxOmran() throws Exception {
        CfgTranche t = creerTrancheMinimale();

        assertAll("délais",
                () -> assertThat(t.getAgreementFirstDelay()).isEqualTo(60),
                () -> assertThat(t.getAgreementSecondDelay()).isEqualTo(30),
                () -> assertThat(t.getWithdrawalDelay()).isEqualTo(48),
                () -> assertThat(t.getInterventionMinimumDelai()).isEqualTo(2),
                () -> assertThat(t.getMaxVisitNumber()).isEqualTo(4),
                () -> assertThat(t.getDelayLawyerMinute()).isEqualTo(60),
                () -> assertThat(t.getDelayMinuteAllSettled()).isEqualTo(30),
                () -> assertThat(t.getSyndicPeriod()).isZero(),
                () -> assertThat(t.getWarrantyDelay()).isZero(),
                () -> assertThat(t.getWarrantyDelayAdded()).isZero(),
                () -> assertThat(t.getPaymentBalanceDelay()).isZero(),
                () -> assertThat(t.getMaxEcheanceDelai()).isZero(),
                () -> assertThat(t.getCountItemBasePrevision()).isZero());
    }

    @Test
    void creation_tauxEtEcartsTypesInitiauxOmran() throws Exception {
        CfgTranche t = creerTrancheMinimale();

        assertAll("taux et écarts-types",
                () -> assertThat(t.getEcarttypeConcretisation()).isEqualByComparingTo("5"),
                () -> assertThat(t.getEcarttypeFinalisation()).isEqualByComparingTo("3"),
                () -> assertThat(t.getAdvancePercent()).isEqualByComparingTo("0"),
                () -> assertThat(t.getConcretisationPercent()).isEqualByComparingTo("0"),
                () -> assertThat(t.getAdvancePercentDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getCancellationPenalityPercent()).isEqualByComparingTo("0"),
                () -> assertThat(t.getCancellationPenalityPercentDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getAgreementFirstPenaltyPercent()).isEqualByComparingTo("0"),
                () -> assertThat(t.getAgreementSecondPenaltyPercent()).isEqualByComparingTo("0"),
                () -> assertThat(t.getCreditPercent()).isEqualByComparingTo("0"),
                () -> assertThat(t.getBaseEcarttypeConcretisation()).isEqualByComparingTo("0"),
                () -> assertThat(t.getBaseEcarttypeFinalisation()).isEqualByComparingTo("0"),
                () -> assertThat(t.getBaseConcretisationPercent()).isEqualByComparingTo("0"));
    }

    @Test
    void creation_montantsInitiauxAZero() throws Exception {
        CfgTranche t = creerTrancheMinimale();

        assertAll("montants",
                () -> assertThat(t.getAdvanceAmount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getBeforeReservationAmount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getBeforeReservationAmountDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getDefaultExpenseAmount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getDefaultExpenseAmountDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getArticleTransferPenality()).isEqualByComparingTo("0"),
                () -> assertThat(t.getArticleTransferPenalityDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getSliceTransferPenality()).isEqualByComparingTo("0"),
                () -> assertThat(t.getSliceTransferPenalityDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getCustomerTransferPenality()).isEqualByComparingTo("0"),
                () -> assertThat(t.getCustomerTransferPenalityDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getOtherExpenseAmount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getOtherExpenseAmountDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getSyndicAmount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getSyndicAmountDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(t.getCoveredFloorArea()).isEqualByComparingTo("0"),
                () -> assertThat(t.getTotalSaleBasePrevision()).isEqualByComparingTo("0"),
                () -> assertThat(t.getUnitConcretisation()).isEqualByComparingTo("0"),
                () -> assertThat(t.getUnitFinalisation()).isEqualByComparingTo("0"),
                () -> assertThat(t.getUnitTurnover()).isEqualByComparingTo("0"),
                () -> assertThat(t.getUnitTurnoverHT()).isEqualByComparingTo("0"));
    }

    @Test
    void creation_textesVidesDatesEtClesNulles() throws Exception {
        CfgTranche t = creerTrancheMinimale();

        assertAll("textes vides, dates et clés nulles",
                () -> assertThat(t.getAddress()).isEmpty(),
                () -> assertThat(t.getCity()).isEmpty(),
                () -> assertThat(t.getDescriptionLong()).isEmpty(),
                () -> assertThat(t.getCommerceTribunal()).isEmpty(),
                () -> assertThat(t.getCancellationRightCode()).isEmpty(),
                () -> assertThat(t.getSavAddress()).isEmpty(),
                () -> assertThat(t.getDeliveryDate()).isNull(),
                () -> assertThat(t.getAdmCountry()).isNull());
    }

    @Test
    void creation_concretisationEgaleAvanceEtHabilitationSurLaTranche() throws Exception {
        Map<String, Object> corps = corpsMinimal("TRA-ECR-2");
        corps.put("advancePercent", 0.3);
        corps.put("estEnVente", false);
        CfgTranche t = relire(creer(corps).andExpect(status().isCreated())
                .andExpect(jsonPath("$.concretisationPercent").value(0.3))
                .andReturn().getResponse().getContentAsString());

        assertAll(
                () -> assertThat(t.getConcretisationPercent()).isEqualByComparingTo("0.3"),
                () -> assertThat(t.getIsForSale()).isFalse());
        assertThat(habilitationRepository.findAll().stream()
                .filter(h -> h.getCfgTranche() != null && h.getCfgTranche().getId().equals(t.getId()))
                .toList())
                .singleElement()
                .satisfies(h -> assertAll(
                        () -> assertThat(h.getAdmUser().getId()).isEqualTo(admin.getId()),
                        () -> assertThat(h.getCfgProject()).isNull()));
    }

    @Test
    void creation_400_creditPercentAuDelaDe100() throws Exception {
        Map<String, Object> corps = corpsMinimal("TRA-ECR-CRED");
        corps.put("creditPercent", new BigDecimal("1.5"));
        creer(corps).andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("creditPercent"))
                .andExpect(jsonPath("$.detail").value("Le crédit max ne doit pas dépasser 100% !"));
    }

    @Test
    void creation_400_tauxNegatif() throws Exception {
        Map<String, Object> corps = corpsMinimal("TRA-ECR-NEG");
        corps.put("tauxPenaliteAnnulation", -0.1);
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value("tauxPenaliteAnnulation"));
    }

    @Test
    void creation_400_delaiNegatif() throws Exception {
        Map<String, Object> corps = corpsMinimal("TRA-ECR-DEL");
        corps.put("delaiRetractation", -1);
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value("delaiRetractation"));
    }

    @Test
    void creation_400_programmeManquant() throws Exception {
        Map<String, Object> corps = corpsMinimal("TRA-ECR-SANS");
        corps.remove(PROJET_ID);
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(PROJET_ID));
    }

    @Test
    void creation_409_codeDejaPrisDansUnAutreProgramme() throws Exception {
        CfgProject autre = fixture.projet("PRJ-TRA-AUTRE", fixture.entite("ENT-TRA-AUTRE", true));
        fixture.tranche("TRA-ECR-GLOBAL", autre);
        creer(corpsMinimal("tra-ecr-global")).andExpect(status().isConflict()).andExpect(jsonPath(CHAMP).value("code"));
    }

    @Test
    void modification_400_changementDeProgramme() throws Exception {
        CfgTranche tranche = fixture.tranche("TRA-ECR-MOVE", projet);
        CfgProject autre = fixture.projet("PRJ-TRA-MOVE", fixture.entite("ENT-TRA-MOVE", true));
        modifier(tranche.getId(), Map.of(PROJET_ID, autre.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Le programme d'une tranche ne se modifie pas."));
    }

    @Test
    void modification_memeProgramme_accepte_etConcretisationSuitAvance() throws Exception {
        CfgTranche tranche = fixture.tranche("TRA-ECR-SAME", projet);
        modifier(tranche.getId(), Map.of(PROJET_ID, projet.getId(), "advancePercent", 0.25,
                "dateOctroiTitresFonciers", "2026-09-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concretisationPercent").value(0.25))
                .andExpect(jsonPath("$.dateOctroiTitresFonciers").value("2026-09-01T00:00:00"))
                .andExpect(jsonPath("$.modifiePar").value(AUTEUR));
    }

    @Test
    void modification_404_idInconnu() throws Exception {
        modifier(UUID.randomUUID(), Map.of("description", "x")).andExpect(status().isNotFound());
    }

    @Test
    void liste_porteProjetIdEtProjetCode() throws Exception {
        CfgTranche tranche = fixture.tranche("TRA-ECR-LISTE", projet);
        mockMvc.perform(get("/api/tranches").param("projetId", projet.getId().toString())
                        .with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + tranche.getId() + "')].projetId")
                        .value(projet.getId().toString()))
                .andExpect(jsonPath("$.content[?(@.id=='" + tranche.getId() + "')].projetCode")
                        .value(CODE_PROGRAMME));
    }
}
