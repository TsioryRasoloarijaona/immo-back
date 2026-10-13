package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.dossier.TransformationProspectClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-lot4 §3 : bascule prospect → client pilotée par {@code StrStatus.BoolField011}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectDevientClientTest {

    private static final String STATUT = "/api/dossiers/{id}/statut";
    private static final String MENTION = "Prospect transformé en client";
    private static final String HISTORIQUE = "/api/dossiers/{id}/historique";
    private static final String DERNIER_EVENEMENT = "$.content[0].description";
    private static final String CONCRETISATION = StatutDossier.CONCRETISATION.getCode();
    private static final String RESERVATION = StatutDossier.RESERVATION.getCode();
    private static final String PROSPECT = "PRO";
    private static final String CLIENT = "CLI";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private StkVocationRepository stkVocationRepository;
    @Autowired
    private StrStatusRepository strStatusRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private CfgTierTypeRepository tierTypeRepository;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComPaymentMethodRepository paymentMethodRepository;
    @Autowired
    private StkItemRepository stkItemRepository;
    @Autowired
    private StkItemTypeRepository stkItemTypeRepository;
    @Autowired
    private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private com.tsix.immocrm.repositories.com.ComFolderCustomerRepository folderCustomerRepository;
    @Autowired
    private com.tsix.immocrm.repositories.cfg.CfgTierKindRepository tierKindRepository;

    private CfgTierType type(String code) {
        return tierTypeRepository.findByCode(code).orElseGet(() -> {
            CfgTierType type = new CfgTierType();
            type.setId(UUID.randomUUID());
            type.setCode(code);
            type.setDescription(code);
            return tierTypeRepository.save(type);
        });
    }

    private StrStatus statut(String code, boolean transforme) {
        StrStatus statut = new StrStatus();
        statut.setId(UUID.randomUUID());
        statut.setCode(code);
        statut.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        statut.setTransformeProspectEnClient(transforme);
        return strStatusRepository.save(statut);
    }

    private CfgTier tiers(String codeType) {
        CfgTier tiers = new CfgTier();
        tiers.setId(UUID.randomUUID());
        tiers.setCode("TIER-PDC-" + UUID.randomUUID());
        tiers.setCfgTierType(type(codeType));
        return tierRepository.save(tiers);
    }

    private ComFolder dossier(StrStatus statutCourant, CfgTier client) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-PDC-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-PDC-" + UUID.randomUUID());
        folder.setStkVocation(vocation);
        folder.setStrStatus(statutCourant);
        folder.setCfgCustomer(client);
        return folderRepository.save(folder);
    }

    private void changerStatut(ComFolder folder, String cible) throws Exception {
        mockMvc.perform(patch(STATUT, folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"" + cible + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value(cible));
    }

    private String codeTypeDe(CfgTier tiers) {
        entityManager.flush();
        entityManager.clear();
        return tierRepository.findById(tiers.getId()).orElseThrow().getCfgTierType().getCode();
    }

    @Test
    void reservationVersConcretisation_prospectDevientClient_etHistoriqueLeDit() throws Exception {
        statut(CONCRETISATION, true);
        type(CLIENT);
        CfgTier client = tiers(PROSPECT);
        ComFolder folder = dossier(statut(RESERVATION, false), client);

        changerStatut(folder, CONCRETISATION);

        assertThat(codeTypeDe(client)).isEqualTo(CLIENT);
        // Deux lignes distinctes : le changement de statut, intact, et la bascule.
        mockMvc.perform(get(HISTORIQUE, folder.getId()))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].description", org.hamcrest.Matchers.containsInAnyOrder(
                        "Statut changé vers CONCRETISATION", MENTION)));
    }

    @Test
    void prospectLocation_devientClientLocation() throws Exception {
        statut(CONCRETISATION, true);
        type("CLIRNT");
        CfgTier client = tiers("PRORNT");
        ComFolder folder = dossier(statut(RESERVATION, false), client);

        changerStatut(folder, CONCRETISATION);

        assertThat(codeTypeDe(client)).isEqualTo("CLIRNT");
    }

    @Test
    void reservationVersFormalise_basculeAussi_carCestLIndicateurQuiDecide() throws Exception {
        statut("FORMALISE", true);
        type(CLIENT);
        CfgTier client = tiers(PROSPECT);
        ComFolder folder = dossier(statut(RESERVATION, false), client);

        changerStatut(folder, "FORMALISE");

        assertThat(codeTypeDe(client)).isEqualTo(CLIENT);
    }

    @Test
    void versReservation_sansIndicateur_prospectResteProspect() throws Exception {
        statut(RESERVATION, false);
        type(CLIENT);
        CfgTier client = tiers(PROSPECT);
        ComFolder folder = dossier(statut("ANNULATION", false), client);

        changerStatut(folder, RESERVATION);

        assertThat(codeTypeDe(client)).isEqualTo(PROSPECT);
        mockMvc.perform(get(HISTORIQUE, folder.getId()))
                .andExpect(jsonPath(DERNIER_EVENEMENT).value("Statut changé vers RESERVATION"));
    }

    /** B3 (FEATURE-vente-socle.md) : plus aucun dossier actif « client » ⇒ retour prospect, comme Sale.cs:ManageTier. */
    @Test
    void concretisationVersReservation_sansAutreDossierActif_repasseEnProspect() throws Exception {
        statut(RESERVATION, false);
        type(PROSPECT);
        CfgTier client = tiers(CLIENT);
        ComFolder folder = dossier(statut(CONCRETISATION, true), client);

        changerStatut(folder, RESERVATION);

        assertThat(codeTypeDe(client)).isEqualTo(PROSPECT);
        mockMvc.perform(get(HISTORIQUE, folder.getId()))
                .andExpect(jsonPath("$.content[*].description", org.hamcrest.Matchers.hasItem(
                        TransformationProspectClient.MENTION_RETOUR_PROSPECT)));
    }

    @Test
    void concretisationVersReservation_autreDossierActifClient_resteClient() throws Exception {
        statut(RESERVATION, false);
        type(PROSPECT);
        StrStatus concretisation = statut(CONCRETISATION, true);
        CfgTier client = tiers(CLIENT);
        dossier(concretisation, client);
        ComFolder folder = dossier(concretisation, client);

        changerStatut(folder, RESERVATION);

        assertThat(codeTypeDe(client)).isEqualTo(CLIENT);
    }

    @Test
    void acquereurAvecDossierActif_devientClient() throws Exception {
        statut(CONCRETISATION, true);
        type(CLIENT);
        CfgTier principal = tiers(PROSPECT);
        CfgTier acquereur = tiers(PROSPECT);
        ComFolder folder = dossier(statut(RESERVATION, false), principal);
        coAcquereur(folder, acquereur);

        changerStatut(folder, CONCRETISATION);

        assertThat(codeTypeDe(acquereur)).isEqualTo(CLIENT);
    }

    @Test
    void desistement_clientSansAutreDossier_redevientProspect_kindVide() throws Exception {
        StrStatus desistement = statut("DESISTEMENT", false);
        desistement.setResiliation(true);
        strStatusRepository.save(desistement);
        type(PROSPECT);
        CfgTier client = tiers(CLIENT);
        com.tsix.immocrm.entities.cfg.CfgTierKind nature = new com.tsix.immocrm.entities.cfg.CfgTierKind();
        nature.setId(UUID.randomUUID());
        nature.setCode("KIND-PDC");
        client.setCfgTierKind(tierKindRepository.save(nature));
        tierRepository.save(client);
        ComFolder folder = dossier(statut(CONCRETISATION, true), client);

        mockMvc.perform(patch(STATUT, folder.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"DESISTEMENT\",\"note\":\"Financement refusé\"}"))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        CfgTier relu = tierRepository.findById(client.getId()).orElseThrow();
        assertThat(relu.getCfgTierType().getCode()).isEqualTo(PROSPECT);
        assertThat(relu.getCfgTierKind()).isNull();
    }

    private void coAcquereur(ComFolder folder, CfgTier acquereur) {
        com.tsix.immocrm.entities.com.ComFolderCustomer lien = new com.tsix.immocrm.entities.com.ComFolderCustomer();
        lien.setId(UUID.randomUUID());
        lien.setComFolder(folder);
        lien.setCfgTier(acquereur);
        lien.setPartPercent(new java.math.BigDecimal("0.5"));
        folderCustomerRepository.save(lien);
    }

    @Test
    void dejaClient_rienNeChange_niMention() throws Exception {
        statut(CONCRETISATION, true);
        CfgTier client = tiers(CLIENT);
        ComFolder folder = dossier(statut(RESERVATION, false), client);

        changerStatut(folder, CONCRETISATION);

        assertThat(codeTypeDe(client)).isEqualTo(CLIENT);
        mockMvc.perform(get(HISTORIQUE, folder.getId()))
                .andExpect(jsonPath(DERNIER_EVENEMENT).value("Statut changé vers CONCRETISATION"));
    }

    @Test
    void creationDossier_statutInitialAvecIndicateur_basculeLeProspect() throws Exception {
        statut(RESERVATION, true);
        type(CLIENT);
        CfgTier prospect = tiers(PROSPECT);
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-PDC-" + UUID.randomUUID());
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-PDC-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);
        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-PDC");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);
        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-PDC");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode("LOT-PDC-" + UUID.randomUUID());
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(item);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-PDC-" + UUID.randomUUID());
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-PDC"}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());
        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        assertThat(codeTypeDe(prospect)).isEqualTo(CLIENT);
    }

    /**
     * Revue lot 4 : StrFolderStatusHistory.Description est un NVARCHAR(255). Une note valide de
     * 240 caractères ne doit pas faire échouer la bascule parce que la mention lui est accolée.
     */
    @Test
    void noteLongueMaisValide_laBasculeReussitEtLaMentionEstEcrite() throws Exception {
        statut(CONCRETISATION, true);
        type(CLIENT);
        CfgTier client = tiers(PROSPECT);
        ComFolder folder = dossier(statut(RESERVATION, false), client);
        String note = "n".repeat(240);

        mockMvc.perform(patch(STATUT, folder.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"" + CONCRETISATION + "\",\"note\":\"" + note + "\"}"))
                .andExpect(status().isOk());

        assertThat(codeTypeDe(client)).isEqualTo(CLIENT);
        mockMvc.perform(get(HISTORIQUE, folder.getId()))
                .andExpect(jsonPath("$.content[*].description",
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString(MENTION))));
    }
}
