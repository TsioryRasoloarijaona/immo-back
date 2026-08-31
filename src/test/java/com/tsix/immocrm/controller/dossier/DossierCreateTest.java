package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierCreateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private StkItemRepository stkItemRepository;

    @Autowired
    private StkItemTypeRepository stkItemTypeRepository;

    @Autowired
    private StkItemCategoryRepository stkItemCategoryRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private ComPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private com.tsix.immocrm.repositories.str.StrStatusRepository strStatusRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-CREATE-TEST");
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-CREATE-TEST");
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-CREATE-TEST");
        stkVocationRepository.save(vocation);
    }

    private StkItem newAvailableItem(String code) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        return stkItemRepository.save(item);
    }

    private ComProspection newProspectionWithProspect() {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-CREATE-TEST");
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-CREATE-TEST");
        prospection.setCfgProspect(prospect);
        return prospectionRepository.save(prospection);
    }

    @Test
    void create_happyPath_linksMultipleArticles() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem main = newAvailableItem("LOT-CREATE-MAIN");
        StkItem annexe = newAvailableItem("LOT-CREATE-ANNEXE");

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s","%s"],"modePaieId":"%s","cin":"101112131415"}
                """.formatted(prospection.getId(), main.getId(), annexe.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articles.length()").value(2))
                .andExpect(jsonPath("$.clientId").value(prospection.getCfgProspect().getId().toString()));

        String prospectJson = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/api/prospects/{id}", prospection.getCfgProspect().getId()))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(
                        com.jayway.jsonpath.JsonPath.<String>read(prospectJson, "$.pieceIdentite.numero"))
                .isEqualTo("101112131415");
    }

    @Test
    void create_setsInitialStatusToReservation() throws Exception {
        com.tsix.immocrm.entities.str.StrStatus reservation = new com.tsix.immocrm.entities.str.StrStatus();
        reservation.setId(UUID.randomUUID());
        reservation.setCode("RESERVATION");
        reservation.setStrConfigEntityId(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID);
        strStatusRepository.save(reservation);

        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-STATUT-INIT-TEST");

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-STATUT-INIT-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-STATUT-INIT"}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        String json = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("RESERVATION"))
                .andReturn().getResponse().getContentAsString();

        // Also traceable in the history (see DossierHistoriqueTest for the endpoint itself).
        String dossierId = com.jayway.jsonpath.JsonPath.read(json, "$.id");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/dossiers/{id}/historique", dossierId))
                .andExpect(jsonPath("$.content[0].type").value("STATUT"))
                .andExpect(jsonPath("$.content[0].description").value("Statut changé vers RESERVATION"));
    }

    @Test
    void create_setsDocDateAndGeneratesCodeFromProjectAndSequence() throws Exception {
        ComProspection prospection = newProspectionWithProspect();

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-CODE-TEST");
        // Manually-assigned id => Spring Data's save() sees a non-null id and calls merge(),
        // which returns a NEW managed instance rather than attaching this one — must reassign
        // and use the returned reference for any further association, or a later flush treats it
        // as a transient/unsaved instance (Hibernate TransientPropertyValueException).
        project = projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-CODE-TEST");
        tranche.setCfgProject(project);
        tranche = trancheRepository.save(tranche);

        StkItem item = newAvailableItem("LOT-CODE-TEST");
        item.setStkHierarchy(tranche);
        stkItemRepository.save(item);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-CODE-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-CODE-TEST"}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateDocument").exists())
                // "PRJ-CODE-TEST 2026/00001" — project code, current year, zero-padded sequence.
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern(
                        "PRJ-CODE-TEST \\d{4}/\\d{5}")));

        // ComFolder.StkItemId (main article back-link) must be set — was missing entirely before
        // this fix, leaving folder.getStkItem() always null (broke FicheReservationService etc.).
        com.tsix.immocrm.entities.com.ComFolder saved = folderRepository.findAll().stream()
                .filter(f -> "LOT-CODE-TEST".equals(f.getStkItem() != null ? f.getStkItem().getCode() : null))
                .findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getStkItem().getCode()).isEqualTo("LOT-CODE-TEST");
    }

    @Test
    void create_missingModePaie_returns400() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-CREATE-NOMODEPAIE");

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"cin":"999"}
                """.formatted(prospection.getId(), item.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingCin_returns400() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-CREATE-NOCIN");

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("ESPECES-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s"}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unknownProspection_returns400() throws Exception {
        String body = """
                {"prospectionId":"%s","articleIds":[]}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_prospectionWithoutProspect_returns400() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-NOPROSPECT");
        prospectionRepository.save(prospection);

        String body = """
                {"prospectionId":"%s","articleIds":[]}
                """.formatted(prospection.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_emptyArticleIds_returns400() throws Exception {
        ComProspection prospection = newProspectionWithProspect();

        String body = """
                {"prospectionId":"%s","articleIds":[]}
                """.formatted(prospection.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_avecPrixEtTranche_calculeMontantsHTTTCEtTotalAPayerAvecSyndic() throws Exception {
        ComProspection prospection = newProspectionWithProspect();

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-MONTANTS-TEST");
        project = projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-MONTANTS-TEST");
        tranche.setCfgProject(project);
        tranche.setSyndicAmount(new java.math.BigDecimal("700.00"));
        tranche.setSyndicPeriod(24); // 700 x 24 = 16 800.00 DH de frais syndic
        tranche = trancheRepository.save(tranche);

        StkItem item = newAvailableItem("LOT-MONTANTS-TEST");
        item.setStkHierarchy(tranche);
        item.setVat(new java.math.BigDecimal("0.20"));
        item.setComSalePrice(new java.math.BigDecimal("1000000.00")); // prix commercial (liste) TTC
        item.setComSalePriceHT(new java.math.BigDecimal("833333.333333"));
        stkItemRepository.save(item);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-MONTANTS-TEST");
        paymentMethodRepository.save(modePaie);

        // Prix négocié 900 000 (< 1 000 000 commercial) => remise de 100 000 (10 %).
        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-MONTANTS-TEST","prix":900000.00}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixAvantRemise").value(1000000.00))
                .andExpect(jsonPath("$.remise").value(10.0))
                .andExpect(jsonPath("$.remiseMontant").value(100000.00))
                .andExpect(jsonPath("$.prixApresRemise").value(900000.00))
                .andExpect(jsonPath("$.prix").value(900000.00))
                // Total à payer = prix négocié (900 000, TTC) + Frais Syndic (16 800) — contrairement
                // à une Offre, dont le total n'inclut jamais le syndic.
                .andExpect(jsonPath("$.totalAPayer").value(916800.00));
    }

    @Test
    void create_articleAlreadyOccupied_returns400() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem occupied = newAvailableItem("LOT-OCCUPIED");
        occupied.setComFolderId(UUID.randomUUID());
        stkItemRepository.save(occupied);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"]}
                """.formatted(prospection.getId(), occupied.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
