package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/dossiers} auto-generates the "Schéma de financement" (AUTO-FINANCEMENT /
 * AVANCE / FRAIS SYNDIC), confirmed against real Omran data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierFinancingSchemaTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private com.tsix.immocrm.fixtures.FixtureStatutsDossier fixtureStatuts;
    @Autowired
    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void statutInitial() {
        fixtureStatuts.reservation();
    }

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

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
    private ComSettlementCategoryRepository settlementCategoryRepository;

    private ComSettlementCategory newCategory(String code, StkVocation vocation, Boolean excludeFromPrice) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription(code);
        categorie.setStkVocation(vocation);
        categorie.setExcludeFromPrice(excludeFromPrice);
        return settlementCategoryRepository.save(categorie);
    }

    @Test
    void create_withPrixAndTrancheConfig_generatesSchemaFinancementLines() throws Exception {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SCHEMA-TEST");
        stkVocationRepository.save(vocation);

        newCategory("AUTO-FINANCEMENT", vocation, false);
        newCategory("AVANCE", vocation, false);
        newCategory("FRAIS SYNDIC", vocation, true);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-SCHEMA-TEST");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-SCHEMA-TEST");
        tranche.setCfgProject(project);
        tranche.setAdvancePercent(new BigDecimal("0.300000"));
        tranche.setSyndicAmount(new BigDecimal("700.000000"));
        tranche.setSyndicPeriod(24);
        trancheRepository.save(tranche);

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SCHEMA-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-SCHEMA-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-SCHEMA-TEST");
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-SCHEMA-TEST");
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-SCHEMA-TEST");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-SCHEMA-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"prix":1548000,"modePaieId":"%s","cin":"CIN-SCHEMA-TEST"}
                """.formatted(prospection.getId(), article.getId(), modePaie.getId());

        String createdJson = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = JsonPath.read(createdJson, "$.id").toString();

        String echeancierJson = mockMvc.perform(get("/api/dossiers/{id}/echeancier", dossierId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number avance = (Number) JsonPath.<net.minidev.json.JSONArray>read(echeancierJson,
                "$.content[?(@.categorie=='AVANCE')].montant").get(0);
        Number autoFinancement = (Number) JsonPath.<net.minidev.json.JSONArray>read(echeancierJson,
                "$.content[?(@.categorie=='AUTO-FINANCEMENT')].montant").get(0);
        Number fraisSyndic = (Number) JsonPath.<net.minidev.json.JSONArray>read(echeancierJson,
                "$.content[?(@.categorie=='FRAIS SYNDIC')].montant").get(0);
        Boolean fraisExclu = (Boolean) JsonPath.<net.minidev.json.JSONArray>read(echeancierJson,
                "$.content[?(@.categorie=='FRAIS SYNDIC')].exclureDuPrix").get(0);

        // 1 548 000 * 0.30 = 464 400 ; auto-financement = 1 548 000 - 464 400 = 1 083 600
        // frais syndic = 700 * 24 = 16 800 — all three match the real Omran example exactly.
        assertThat(avance.doubleValue()).isEqualTo(464400.0);
        assertThat(autoFinancement.doubleValue()).isEqualTo(1083600.0);
        assertThat(fraisSyndic.doubleValue()).isEqualTo(16800.0);
        assertThat(fraisExclu).isTrue();
    }

    @Test
    void create_withRemisePercent_schemaCalculeSurLePrixApresRemise() throws Exception {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SCHEMA-REMISE");
        stkVocationRepository.save(vocation);

        newCategory("AUTO-FINANCEMENT", vocation, false);
        newCategory("AVANCE", vocation, false);
        newCategory("FRAIS SYNDIC", vocation, true);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-SCHEMA-REMISE");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-SCHEMA-REMISE");
        tranche.setCfgProject(project);
        tranche.setAdvancePercent(new BigDecimal("0.300000"));
        tranche.setSyndicAmount(new BigDecimal("700.000000"));
        tranche.setSyndicPeriod(24);
        trancheRepository.save(tranche);

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SCHEMA-REMISE");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-SCHEMA-REMISE");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-SCHEMA-REMISE");
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-SCHEMA-REMISE");
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-SCHEMA-REMISE");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-SCHEMA-REMISE");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"prix":1548000,"remisePercent":10,"modePaieId":"%s","cin":"CIN-SCHEMA-REMISE"}
                """.formatted(prospection.getId(), article.getId(), modePaie.getId());

        String createdJson = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = JsonPath.read(createdJson, "$.id").toString();

        String echeancierJson = mockMvc.perform(get("/api/dossiers/{id}/echeancier", dossierId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number avance = (Number) JsonPath.<net.minidev.json.JSONArray>read(echeancierJson,
                "$.content[?(@.categorie=='AVANCE')].montant").get(0);
        Number autoFinancement = (Number) JsonPath.<net.minidev.json.JSONArray>read(echeancierJson,
                "$.content[?(@.categorie=='AUTO-FINANCEMENT')].montant").get(0);
        Number fraisSyndic = (Number) JsonPath.<net.minidev.json.JSONArray>read(echeancierJson,
                "$.content[?(@.categorie=='FRAIS SYNDIC')].montant").get(0);

        // Prix avant remise 1 548 000, remise 10 % => prix dû 1 393 200.
        // AVANCE   = 1 393 200 * 0.30 = 417 960  (et non 464 400 : la remise se répercute)
        // AUTO-FIN = 1 393 200 - 417 960 = 975 240
        // FRAIS SYNDIC = 700 * 24 = 16 800 — barème de la tranche, jamais remisé.
        assertThat(avance.doubleValue()).isEqualTo(417960.0);
        assertThat(autoFinancement.doubleValue()).isEqualTo(975240.0);
        assertThat(fraisSyndic.doubleValue()).isEqualTo(16800.0);
    }

    @Test
    void create_withoutPrix_generatesNoSchemaLines() throws Exception {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SCHEMA-NOPRIX");
        stkVocationRepository.save(vocation);

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SCHEMA-NOPRIX");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-SCHEMA-NOPRIX");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-SCHEMA-NOPRIX");
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-SCHEMA-NOPRIX");
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-SCHEMA-NOPRIX");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-SCHEMA-NOPRIX");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-SCHEMA-NOPRIX"}
                """.formatted(prospection.getId(), article.getId(), modePaie.getId());

        String createdJson = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = JsonPath.read(createdJson, "$.id").toString();

        mockMvc.perform(get("/api/dossiers/{id}/echeancier", dossierId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.totalElements").value(0));
    }
}
