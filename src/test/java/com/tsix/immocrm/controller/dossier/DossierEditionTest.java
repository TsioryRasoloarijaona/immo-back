package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ActionDossierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fiche du dossier modifiable — FEATURE-vente-edition.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierEditionTest {

    private static final String MODIFIER = "/api/dossiers/{id}";
    private static final String DROITS = "/api/dossiers/{id}/edition";
    private static final String HISTORIQUE = "/api/dossiers/{id}/historique";
    private static final String ADMIN = "Administrateur";
    private static final String CHEF = "Commercial Chef";
    private static final String DIRECTION = "Direction Générale";
    private static final String COMMERCIAL = "Commercial";
    private static final String MODIFIES = "$.champsModifies";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private FixtureInteraction fixtureInteraction;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTierTypeRepository tierTypeRepository;
    @Autowired private StrStatusRepository statusRepository;
    @Autowired private ActionDossierRepository actionRepository;
    @Autowired private EntityManager em;

    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        dossier = fixture.dossier("DOS-EDI", fixture.tranche("EDI-TR", true), StatutDossier.CONCRETISATION);
    }

    private ResultActions modifier(ComFolder d, String role, String corps) throws Exception {
        return mockMvc.perform(patch(MODIFIER, d.getId()).with(user("edition").roles(role))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ComFolder relu(ComFolder d) {
        em.flush();
        em.clear();
        return folderRepository.findById(d.getId()).orElseThrow();
    }

    // ------------------------------------------------------------------ dates

    @Test
    void dates_parLaDirectionGenerale_ecritSigneEtHistorise() throws Exception {
        modifier(dossier, DIRECTION, """
                {"minute":"2025-03-01T00:00:00","promesseClient":"2025-02-01T00:00:00","numeroTitre":" T-42 "}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiche.dates.minute").value("2025-03-01T00:00:00"))
                .andExpect(jsonPath(MODIFIES, hasItem("numeroTitre")));

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getAgreementSavingDate()).isEqualTo(LocalDateTime.of(2025, 3, 1, 0, 0)),
                () -> assertThat(f.getReservationDateCustomer()).isEqualTo(LocalDateTime.of(2025, 2, 1, 0, 0)),
                () -> assertThat(f.getFinalTitleNumber()).isEqualTo("T-42"),
                () -> assertThat(f.getUserUpdateDateTime()).isNotNull());
        mockMvc.perform(get(HISTORIQUE, dossier.getId()).with(user("lecteur").roles(ADMIN)))
                .andExpect(jsonPath("$.content[*].description",
                        hasItem("Modification : minute, promesseClient, numeroTitre")));
    }

    @Test
    void dates_champsAVider_remetANull() throws Exception {
        dossier.setAgreementSavingDate(LocalDateTime.of(2025, 3, 1, 0, 0));
        folderRepository.save(dossier);

        modifier(dossier, CHEF, "{\"champsAVider\":[\"minute\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath(MODIFIES + "[0]").value("minute"));

        assertThat(relu(dossier).getAgreementSavingDate()).isNull();
    }

    @Test
    void champsAVider_clientInterdit_400() throws Exception {
        modifier(dossier, ADMIN, "{\"champsAVider\":[\"clientId\"]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void corpsSansChamp_400() throws Exception {
        modifier(dossier, ADMIN, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void verifDates_concretisationAvantLaDateDuDossier_400MessageOmran() throws Exception {
        dossier.setDocDate(LocalDateTime.of(2025, 1, 10, 9, 0));
        folderRepository.save(dossier);
        em.flush();

        modifier(dossier, ADMIN, "{\"concretisation\":\"2025-01-05T00:00:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La date 'Date Concrétisation / Promesse de vente: 05/01/2025' "
                        + "doit être supérieur ou égal à la date 'Date dossier: 10/01/2025'."));

        // La transaction de test survit au refus : ses changements en mémoire sont abandonnés comme au rollback.
        em.clear();
        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getAdvanceDate()).isNull();
    }

    // ------------------------------------------------------------------ 403 par groupe

    @ParameterizedTest(name = "{0} ne modifie pas {1}")
    @CsvSource(delimiter = '|', value = {
            "Commercial|{\"minute\":\"2025-03-01T00:00:00\"}",
            "Commercial|{\"annulationCauses\":\"x\"}",
            "Commercial Chef|{\"clientId\":\"00000000-0000-0000-0000-000000000001\"}",
            "Commercial Chef|{\"commercialId\":\"00000000-0000-0000-0000-000000000001\"}",
            "Direction Générale|{\"gestionnaire1Id\":\"00000000-0000-0000-0000-000000000001\"}",
            "Direction Générale|{\"notaireId\":\"00000000-0000-0000-0000-000000000001\"}",
            "Direction Générale|{\"agenceCredit\":\"Agence\"}",
            "Commercial|{\"ancienClient\":true}",
            "Commercial Chef|{\"penalitesDesactivees\":true}",
            "Commercial Chef|{\"remisePourcentage\":5}",
            "Commercial Chef|{\"tva\":0.1}"
    })
    void groupeInterditAuRole_403_rienNEstEcrit(String role, String corps) throws Exception {
        modifier(dossier, role, corps).andExpect(status().isForbidden());

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getUserUpdateDateTime()).isNull(),
                () -> assertThat(f.getVATRatio()).isEqualByComparingTo("0.2"));
    }

    @Test
    void unChampInterditParmiDesChampsPermis_403() throws Exception {
        modifier(dossier, CHEF, "{\"minute\":\"2025-03-01T00:00:00\",\"remisePourcentage\":5}")
                .andExpect(status().isForbidden());

        assertThat(relu(dossier).getAgreementSavingDate()).isNull();
    }

    // ------------------------------------------------------------------ acteurs, crédit, options

    @Test
    void gestionnairesNotaireCreditOptions_parLeChef() throws Exception {
        CfgTier gestionnaire = fixture.tier("GES-EDI");
        CfgTier notaire = fixture.tier("NOT-EDI");
        CfgTier banque = fixture.tier("BAN-EDI");
        String corps = """
                {"gestionnaire1Id":"%s","notaireId":"%s","banqueCreditId":"%s","agenceCredit":"Agence centre",
                 "meilleurTauxEnvoi":"2025-04-01T00:00:00","ancienClient":true,"cadeauPlusTard":true}"""
                .formatted(gestionnaire.getId(), notaire.getId(), banque.getId());

        modifier(dossier, CHEF, corps)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiche.autres.notaireId").value(notaire.getId().toString()))
                .andExpect(jsonPath("$.fiche.autres.agenceCredit").value("Agence centre"));

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getCfgSaleManager().getId()).isEqualTo(gestionnaire.getId()),
                () -> assertThat(f.getCreditBank().getId()).isEqualTo(banque.getId()),
                () -> assertThat(f.getLoanCompanySendDate()).isEqualTo(LocalDateTime.of(2025, 4, 1, 0, 0)),
                () -> assertThat(f.getIsOldCustomer()).isTrue(),
                () -> assertThat(f.getIsWithGiftChoiceLater()).isTrue());
    }

    @Test
    void tiersInconnu_400() throws Exception {
        modifier(dossier, CHEF, "{\"notaireId\":\"%s\"}".formatted(UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commercialEtPenalites_parLAdministrateur() throws Exception {
        CfgTier commercial = fixture.tier("COM-EDI");

        modifier(dossier, ADMIN, "{\"commercialId\":\"%s\",\"penalitesDesactivees\":true}".formatted(commercial.getId()))
                .andExpect(status().isOk());

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getCfgCommercial().getId()).isEqualTo(commercial.getId()),
                () -> assertThat(f.getIsPenaltyDisabled()).isTrue());
    }

    // ------------------------------------------------------------------ prix

    /** Cas réel APF 2025/00320 : total HT 1 425 000, remise 425 000 HT, plafonds de l'article 3,70 % / 7,41 %. */
    private void dossierApf202500320() {
        dossier.setUnitCostHT(new BigDecimal("14250"));
        folderRepository.save(dossier);
        StkItem article = dossier.getStkItem();
        article.setDiscountMax(new BigDecimal("0.037037"));
        article.setDiscountMax2(new BigDecimal("0.074074"));
        stkItemRepository.save(article);
    }

    @Test
    void remise_auDelaDuDeuxiemeNiveau_400MessageOmran_puisForcee() throws Exception {
        dossierApf202500320();

        modifier(dossier, ADMIN, "{\"remiseMontantHT\":425000}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Vous ne pouvez pas dépasser le deuxième niveau de remise pour cet article (7,41 %) !"));
        assertThat(relu(dossier).getDiscountAmount()).isNull();

        modifier(dossier, ADMIN, "{\"remiseMontantHT\":425000,\"forcerRemise\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath(MODIFIES, hasItem("remise")))
                .andExpect(jsonPath("$.ecartSchema").value(1200000.0));

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("425000"),
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0.298246"),
                () -> assertThat(f.getPriceBeforeDiscountHT()).isEqualByComparingTo("1425000"),
                () -> assertThat(f.getPriceAfterDiscountHT()).isEqualByComparingTo("1000000"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1200000"),
                () -> assertThat(f.getDiscountChangeDate()).isNotNull());
    }

    @Test
    void remiseEnPourcentage_sousLePlafond_tauxUniqueEtDateDeChangement() throws Exception {
        dossierApf202500320();

        modifier(dossier, ADMIN, "{\"remisePourcentage\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiche.enTete.remiseMontantHT").value(71250.0));

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0.05"),
                () -> assertThat(f.getPriceAfterDiscountHT()).isEqualByComparingTo("1353750"),
                () -> assertThat(f.getDiscountChangeDate()).isNotNull());
    }

    @Test
    void tvaSeule_montantDeRemiseConserve_pasDeDateDeChangement() throws Exception {
        dossier.setDiscountAmount(new BigDecimal("50000"));
        folderRepository.save(dossier);

        modifier(dossier, ADMIN, "{\"tva\":0.1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath(MODIFIES + "[0]").value("tva"));

        ComFolder f = relu(dossier);
        assertAll(
                () -> assertThat(f.getVATRatio()).isEqualByComparingTo("0.1"),
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("50000"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1045000"),
                () -> assertThat(f.getDiscountChangeDate()).isNull());
    }

    @Test
    void remiseMontantEtPourcentageEnsemble_400() throws Exception {
        modifier(dossier, ADMIN, "{\"remiseMontantHT\":1000,\"remisePourcentage\":5}")
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ note

    @Test
    void note_parUnCommercial_devientUneActionSystemeNote() throws Exception {
        fixtureInteraction.typeNote();
        fixture.categoriesAction();

        modifier(dossier, COMMERCIAL, "{\"note\":\"Client injoignable cette semaine\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiche.autres.note").value("Client injoignable cette semaine"));

        em.flush();
        assertThat(actionRepository.actionsDuDossier(dossier.getId())).singleElement()
                .satisfies(a -> assertAll(
                        () -> assertThat(a.getAlertMessage()).isEqualTo("Client injoignable cette semaine"),
                        () -> assertThat(a.getIsSystem()).isTrue(),
                        () -> assertThat(a.getComActionMessageType().getCode()).isEqualTo("Note")));
    }

    @Test
    void noteInchangee_aucuneNouvelleAction() throws Exception {
        fixtureInteraction.typeNote();
        dossier.setNotes("Déjà là");
        folderRepository.save(dossier);

        modifier(dossier, COMMERCIAL, "{\"note\":\"Déjà là\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath(MODIFIES).isEmpty());

        em.flush();
        assertThat(actionRepository.actionsDuDossier(dossier.getId())).isEmpty();
    }

    // ------------------------------------------------------------------ dossier sorti

    @Test
    void dossierSorti_horsAnnulation_409_annulationAcceptee() throws Exception {
        ComFolder annule = fixture.dossier("DOS-EDI-ANN", fixture.tranche("EDI-ANN", true), StatutDossier.ANNULATION);

        modifier(annule, ADMIN, "{\"minute\":\"2025-03-01T00:00:00\"}")
                .andExpect(status().isConflict());
        modifier(annule, ADMIN, "{\"annulationDate\":\"2025-05-02T00:00:00\",\"annulationCauses\":\"Financement refusé\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiche.annulation.causes").value("Financement refusé"));

        ComFolder f = relu(annule);
        assertAll(
                () -> assertThat(f.getCancelDate()).isEqualTo(LocalDateTime.of(2025, 5, 2, 0, 0)),
                () -> assertThat(f.getAgreementSavingDate()).isNull());
    }

    @Test
    void droits_chefSurDossierActif_etAdministrateurSurDossierSorti() throws Exception {
        mockMvc.perform(get(DROITS, dossier.getId()).with(user("chef").roles(CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sorti").value(false))
                .andExpect(jsonPath("$.groupes", hasItem("GESTIONNAIRES")))
                .andExpect(jsonPath("$.groupes", org.hamcrest.Matchers.not(hasItem("PRIX"))));

        ComFolder annule = fixture.dossier("DOS-EDI-DR", fixture.tranche("EDI-DR", true), StatutDossier.DESISTEMENT);
        mockMvc.perform(get(DROITS, annule.getId()).with(user("admin").roles(ADMIN)))
                .andExpect(jsonPath("$.sorti").value(true))
                .andExpect(jsonPath("$.groupes.length()").value(1))
                .andExpect(jsonPath("$.groupes[0]").value("ANNULATION"));
    }

    @Test
    void dossierInconnu_404() throws Exception {
        mockMvc.perform(patch(MODIFIER, UUID.randomUUID()).with(user("admin").roles(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"minute\":\"2025-03-01T00:00:00\"}"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ changement de client

    private CfgTierType type(String code) {
        return tierTypeRepository.findByCode(code).orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode(code);
            t.setDescription(code);
            return tierTypeRepository.save(t);
        });
    }

    @Test
    void changementDeClient_nouveauDevientClient_ancienSansDossierActifRedevientProspect() throws Exception {
        dossier.getStrStatus().setTransformeProspectEnClient(true);
        statusRepository.save(dossier.getStrStatus());
        CfgTier ancien = dossier.getCfgCustomer();
        ancien.setCfgTierType(type("CLI"));
        tierRepository.save(ancien);
        CfgTier nouveau = fixture.tier("NOUV-EDI");
        nouveau.setCfgTierType(type("PRO"));
        tierRepository.save(nouveau);

        modifier(dossier, ADMIN, "{\"clientId\":\"%s\"}".formatted(nouveau.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(MODIFIES + "[0]").value("clientId"));

        em.flush();
        em.clear();
        assertAll(
                () -> assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getCfgCustomer().getId())
                        .isEqualTo(nouveau.getId()),
                () -> assertThat(tierRepository.findById(nouveau.getId()).orElseThrow().getCfgTierType().getCode())
                        .isEqualTo("CLI"),
                () -> assertThat(tierRepository.findById(ancien.getId()).orElseThrow().getCfgTierType().getCode())
                        .isEqualTo("PRO"));
        mockMvc.perform(get(HISTORIQUE, dossier.getId()).with(user("lecteur").roles(ADMIN)))
                .andExpect(jsonPath("$.content[*].description", hasItem(containsString("Retour prospect : "))));
    }

    @Test
    void changementDeClient_parLeChef_403() throws Exception {
        CfgTier nouveau = fixture.tier("NOUV-EDI2");

        modifier(dossier, CHEF, "{\"clientId\":\"%s\"}".formatted(nouveau.getId()))
                .andExpect(status().isForbidden());

        assertThat(relu(dossier).getCfgCustomer().getId()).isNotEqualTo(nouveau.getId());
    }
}
