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
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-lot4 §1 : prix commercial à 0 → prix plancher, partout où le prix sert de base. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class PrixPlancherTest {

    private static final String DOSSIERS = "/api/dossiers";
    private static final String DEVIS = "/api/prospections/{id}/devis";
    private static final String ARTICLE = "/api/articles/{id}";
    private static final String PRIX_SUGGERE = "$.prixSuggere";
    private static final String EST_PLANCHER = "$.prixSuggereEstPlancher";
    private static final String PLANCHER = "900000";

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
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

    private CfgTranche tranche;
    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;
    private ComPaymentMethod modePaie;

    @Autowired
    private com.tsix.immocrm.fixtures.FixtureStatutDossier fixtureStatutDossier;

    @BeforeEach
    void setUp() {
        fixtureStatutDossier.reservation();
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-PLANCHER");
        project = projectRepository.save(project);

        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-PLANCHER");
        tranche.setCfgProject(project);
        tranche = trancheRepository.save(tranche);

        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-PLANCHER");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-PLANCHER");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-PLANCHER");
        stkVocationRepository.save(vocation);

        modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-PLANCHER");
        paymentMethodRepository.save(modePaie);
    }

    private StkItem article(String code, String prix, String plancher) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        item.setStkHierarchy(tranche);
        item.setVat(new BigDecimal("0.20"));
        item.setComSalePrice(new BigDecimal(prix));
        item.setComSalePriceHT(BigDecimal.ZERO);
        item.setComSalePriceFloor(new BigDecimal(plancher));
        return stkItemRepository.save(item);
    }

    private ComProspection prospection(String code) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("TIER-" + code);
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code);
        prospection.setCfgProspect(prospect);
        return prospectionRepository.save(prospection);
    }

    private String corpsDossier(ComProspection prospection, StkItem item, String prix) {
        return """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-PLANCHER","prix":%s}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId(), prix);
    }

    @Test
    void creationDossier_prixCommercialNul_prixArticleEgalAuPlancher_remiseNulle() throws Exception {
        StkItem item = article("LOT-PLANCHER-1", "0", PLANCHER);

        mockMvc.perform(post(DOSSIERS).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDossier(prospection("PSN-PLANCHER-1"), item, PLANCHER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixAvantRemise").value(900000.0))
                .andExpect(jsonPath("$.remiseMontant").value(0.0))
                .andExpect(jsonPath("$.prixApresRemise").value(900000.0));
    }

    @Test
    void creationDossier_prixEtPlancherNuls_baseEgalePrixNegocie() throws Exception {
        StkItem item = article("LOT-PLANCHER-2", "0", "0");

        mockMvc.perform(post(DOSSIERS).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDossier(prospection("PSN-PLANCHER-2"), item, "650000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixAvantRemise").value(650000.0))
                .andExpect(jsonPath("$.remiseMontant").value(0.0));
    }

    @Test
    void devisSansPrixImpose_partDuPlancher() throws Exception {
        StkItem item = article("LOT-PLANCHER-3", "0", PLANCHER);
        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + item.getId() + "\"}";

        mockMvc.perform(post(DEVIS, prospection("PSN-PLANCHER-3").getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montants.prixArticleTTC").value(900000))
                .andExpect(jsonPath("$.montants.prixArticleHT").value(750000))
                .andExpect(jsonPath("$.montants.prixVenteTTC").value(900000));
    }

    @Test
    void devisAvecPrixImpose_prixImposePrioritaire() throws Exception {
        StkItem item = article("LOT-PLANCHER-4", "0", PLANCHER);
        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + item.getId()
                + "\",\"prix\":950000}";

        mockMvc.perform(post(DEVIS, prospection("PSN-PLANCHER-4").getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montants.prixArticleTTC").value(950000));
    }

    @Test
    void ficheArticle_exposeLePrixSuggereEtSonOrigine() throws Exception {
        StkItem plancher = article("LOT-PLANCHER-5", "0", PLANCHER);
        StkItem commercial = article("LOT-PLANCHER-6", "1100000", PLANCHER);
        StkItem sansPrix = article("LOT-PLANCHER-7", "0", "0");

        mockMvc.perform(get(ARTICLE, plancher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PRIX_SUGGERE).value(900000.0))
                .andExpect(jsonPath(EST_PLANCHER).value(true))
                .andExpect(jsonPath("$.comSalePrice").value(0.0));
        mockMvc.perform(get(ARTICLE, commercial.getId()))
                .andExpect(jsonPath(PRIX_SUGGERE).value(1100000.0))
                .andExpect(jsonPath(EST_PLANCHER).value(false));
        mockMvc.perform(get(ARTICLE, sansPrix.getId()))
                .andExpect(jsonPath(PRIX_SUGGERE).doesNotExist())
                .andExpect(jsonPath(EST_PLANCHER).value(false));
    }

    @Test
    void listeArticlesDeLaTranche_exposeLePrixSuggere() throws Exception {
        article("LOT-PLANCHER-8", "0", PLANCHER);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].prixSuggere").value(900000.0))
                .andExpect(jsonPath("$.content[0].prixSuggereEstPlancher").value(true));
    }
}
