package com.tsix.immocrm.controller.patrimoine;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tsix.immocrm.domain.TypeTiersPatrimoine;
import com.tsix.immocrm.entities.act.ActAccount;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTrancheStanding;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.stk.StkTrancheHeading;
import com.tsix.immocrm.fixtures.FixtureFichePatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-tranche-fiche.md §4.1, §4.6 et §6. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheFicheTest {

    private static final String CHAMP = "$.champ";
    private static final String CHAMPS_A_VIDER = "champsAVider";
    private static final String STANDING_ID = "standingId";
    private static final String DATE_AFH = "dateMainLeveeAfh";
    private static final String VISIBLE_SAV = "visibleSav";
    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();

    /**
     * Colonnes non NULL sur la ligne modèle {@code APF-HIDDEN} de la production, hors dates et clés étrangères :
     * aucune ne doit devenir NULL, même après {@code champsAVider} (FEATURE-patrimoine-ecriture.md §2.4).
     */
    private static final String COLONNES_NULLES = """
            SELECT COUNT(*) FROM CfgTranche WHERE PKey = :id AND (
              Code IS NULL OR Description IS NULL OR Address IS NULL OR Email IS NULL OR Mobile IS NULL
              OR Phone IS NULL OR Fax IS NULL OR WebSite IS NULL OR Address2 IS NULL OR Address3 IS NULL
              OR ZipCode IS NULL OR City IS NULL OR AdvancePercent IS NULL OR ConcretisationPercent IS NULL
              OR EcarttypeFinalisation IS NULL OR EcarttypeConcretisation IS NULL OR IsForSale IS NULL
              OR IsForRent IS NULL OR IsForProject IS NULL OR IsForBuy IS NULL OR IsForSav IS NULL
              OR BeforeReservationAmount IS NULL OR SyndicAmount IS NULL OR SyndicPeriod IS NULL
              OR WarrantyDelayAdded IS NULL OR WarrantyDelay IS NULL OR AgreementFirstDelay IS NULL
              OR AgreementSecondDelay IS NULL OR PaymentBalanceDelay IS NULL
              OR BeforeReservationAmountDiscount IS NULL OR AdvancePercentDiscount IS NULL
              OR DefaultExpenseAmountDiscount IS NULL OR DefaultExpenseAmount IS NULL
              OR CancellationPenalityPercent IS NULL OR CancellationPenalityPercentDiscount IS NULL
              OR ArticleTransferPenality IS NULL OR ArticleTransferPenalityDiscount IS NULL
              OR SliceTransferPenality IS NULL OR SliceTransferPenalityDiscount IS NULL
              OR CustomerTransferPenality IS NULL OR CustomerTransferPenalityDiscount IS NULL
              OR SyndicAmountDiscount IS NULL OR OtherExpenseAmount IS NULL OR OtherExpenseAmountDiscount IS NULL
              OR CancellationPenalityBasedOnItemPrice IS NULL OR AgreementFirstPenaltyPercent IS NULL
              OR AgreementSecondPenaltyPercent IS NULL OR CommerceTribunal IS NULL OR CancellationRightCode IS NULL
              OR DescriptionLong IS NULL OR DelayLawyerMinute IS NULL OR DelayMinuteAllSettled IS NULL
              OR UnitConcretisation IS NULL OR UnitFinalisation IS NULL OR UnitTurnover IS NULL
              OR CoveredFloorArea IS NULL OR SavAddress IS NULL OR IsWithGiftForProposer IS NULL
              OR IsWithGiftForOldCustomer IS NULL OR InterventionMinimumDelai IS NULL OR MaxVisitNumber IS NULL
              OR IsWithGiftForProspectionOrigin IS NULL OR AccumulatedGifts IS NULL
              OR IsActiveAdvancePercentDiscount IS NULL OR WithdrawalDelay IS NULL OR AdvanceAmount IS NULL
              OR AdvanceBasedOnAmount IS NULL OR IsForPrm IS NULL OR IsForRom IS NULL OR UnitTurnoverHT IS NULL
              OR IsWithVerifMaxEcheanceDelai IS NULL OR MaxEcheanceDelai IS NULL OR IsWithVerifCredit IS NULL
              OR CreditPercent IS NULL)
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureFichePatrimoine fiche;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private CfgCompany entite;
    private CfgProject projet;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-TRA-FICHE");
        entite = fixture.entite("ENT-TRA-FICHE", true);
        projet = fixture.projet("PRJ-TRA-FICHE", entite);
    }

    private Map<String, Object> corps(String code) {
        Map<String, Object> corps = new HashMap<>();
        corps.put("projetId", projet.getId());
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

    private UUID creerMinimale(String code) throws Exception {
        String reponse = creer(corps(code)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JSON.readTree(reponse).get("id").asText());
    }

    @Test
    void creation_nouveauxChampsEcritsEtRelus() throws Exception {
        CfgTrancheStanding standing = fiche.standing("STD-TRA-FICHE");
        LndLevel niveau = fiche.niveau("R+5-TRA");
        StkTrancheHeading rubrique = fiche.rubrique("RUB-TRA", 1);
        CfgTier responsable = fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-TRA-SAV");
        CfgTier fournisseur = fiche.tiers(TypeTiersPatrimoine.FOURNISSEUR, "FOU-TRA-AUT");
        PrjProject projetChantier = fiche.projetChantier("PRJ-CH-TRA", entite);
        ActAccount compte = fiche.compte("4111TRA");
        Map<String, Object> corps = corps("TRA-FICHE-1");
        corps.put(STANDING_ID, standing.getId());
        corps.put("niveauTerrainId", niveau.getId());
        corps.put("rubriqueId", rubrique.getId());
        corps.put("responsableSavId", responsable.getId());
        corps.put("tiersAutruiId", fournisseur.getId());
        corps.put("projetFacturationId", projetChantier.getId());
        corps.put("planComptableId", compte.getId());
        corps.put("adresseSav", "Bureau SAV");
        corps.put(VISIBLE_SAV, true);
        corps.put("visibleAchat", true);
        corps.put("cadeauParrain", true);
        corps.put("cadeauxCumules", true);
        corps.put(DATE_AFH, "2026-03-01T00:00:00");
        corps.put("dateRepartitionCharges", "2026-04-01T00:00:00");

        creer(corps)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.standingLibelle").value("Standing STD-TRA-FICHE"))
                .andExpect(jsonPath("$.niveauTerrainLibelle").value("Niveau R+5-TRA"))
                .andExpect(jsonPath("$.rubriqueLibelle").value("Rubrique RUB-TRA"))
                .andExpect(jsonPath("$.responsableSavLibelle").value("Tiers EMP-TRA-SAV"))
                .andExpect(jsonPath("$.tiersAutruiLibelle").value("Tiers FOU-TRA-AUT"))
                .andExpect(jsonPath("$.projetFacturationLibelle").value("Projet PRJ-CH-TRA"))
                .andExpect(jsonPath("$.planComptableLibelle").value("4111TRA — Compte 4111TRA"))
                .andExpect(jsonPath("$.adresseSav").value("Bureau SAV"))
                .andExpect(jsonPath("$.visibleSav").value(true))
                .andExpect(jsonPath("$.visibleAchat").value(true))
                .andExpect(jsonPath("$.visibleProjet").value(false))
                .andExpect(jsonPath("$.cadeauParrain").value(true))
                .andExpect(jsonPath("$.cadeauxCumules").value(true))
                .andExpect(jsonPath("$.cadeauAncienClient").value(false))
                .andExpect(jsonPath("$.dateMainLeveeAfh").value("2026-03-01T00:00:00"))
                .andExpect(jsonPath("$.dateRepartitionCharges").value("2026-04-01T00:00:00"))
                .andExpect(jsonPath("$.entiteLibelle").value("Entité ENT-TRA-FICHE"))
                .andExpect(jsonPath("$.plancherCouvert").value(0));
    }

    @Test
    void visibilites_modifiablesDansLesDeuxSens() throws Exception {
        UUID id = creerMinimale("TRA-FICHE-VIS");
        modifier(id, Map.of(VISIBLE_SAV, true, "visibleProjet", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleSav").value(true))
                .andExpect(jsonPath("$.visibleProjet").value(true));
        modifier(id, Map.of(VISIBLE_SAV, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibleSav").value(false))
                .andExpect(jsonPath("$.visibleProjet").value(true));
    }

    @Test
    void champsAVider_dateFonciereEtReferenceRemisesANull() throws Exception {
        Map<String, Object> corps = corps("TRA-FICHE-VID");
        corps.put(STANDING_ID, fiche.standing("STD-TRA-VID").getId());
        corps.put(DATE_AFH, "2026-03-01T00:00:00");
        String reponse = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JSON.readTree(reponse).get("id").asText());

        modifier(id, Map.of(CHAMPS_A_VIDER, List.of(DATE_AFH, STANDING_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateMainLeveeAfh").doesNotExist())
                .andExpect(jsonPath("$.standingId").doesNotExist());
    }

    @Test
    void champsAVider_jamaisNullLaOuLaLigneModeleEstRenseignee() throws Exception {
        UUID id = creerMinimale("TRA-FICHE-NUL");
        modifier(id, Map.of(CHAMPS_A_VIDER, List.of("adresse", "adresseSav", "tribunalCommerce", "descriptionLongue",
                "delaiPremierEngagement", "montantSyndic", "creditPercent", "ecartTypeFinalisation", "periodeSyndic")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delaiPremierEngagement").value(0))
                .andExpect(jsonPath("$.adresse").value(""));

        em.flush();
        Number nulles = (Number) em.createNativeQuery(COLONNES_NULLES).setParameter("id", id).getSingleResult();
        assertThat(nulles.intValue()).isZero();
    }

    @Test
    void champsAVider_400_champObligatoire() throws Exception {
        UUID id = creerMinimale("TRA-FICHE-OBL");
        modifier(id, Map.of(CHAMPS_A_VIDER, List.of("projetId")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value(CHAMPS_A_VIDER));
    }

    @Test
    void creation_400_creditSuperieurA100() throws Exception {
        Map<String, Object> corps = corps("TRA-FICHE-CRE");
        corps.put("creditPercent", 1.5);
        creer(corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("creditPercent"))
                .andExpect(jsonPath("$.detail").value("Le crédit max ne doit pas dépasser 100% !"));
    }

    @Test
    void creation_400_standingInconnu() throws Exception {
        Map<String, Object> corps = corps("TRA-FICHE-STD");
        corps.put(STANDING_ID, UUID.randomUUID());
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value(STANDING_ID));
    }

    @Test
    void creation_400_tiersAutruiQuiNEstPasUnFournisseur() throws Exception {
        Map<String, Object> corps = corps("TRA-FICHE-AUT");
        corps.put("tiersAutruiId", fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-TRA-AUT").getId());
        creer(corps).andExpect(status().isBadRequest()).andExpect(jsonPath(CHAMP).value("tiersAutruiId"));
    }

    @Test
    void fiche_404_idInconnu() throws Exception {
        mockMvc.perform(get("/api/tranches/{id}", UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}
