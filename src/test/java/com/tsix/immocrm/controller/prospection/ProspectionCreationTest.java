package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
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
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/prospections} — instructions/from_front.md demande #9, le point d'entrée pour
 * un prospect déjà existant (voir {@code CreerProspectionRequest} pour la distinction avec le
 * flux pré-prospection).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionCreationTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;

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

    private CfgTier prospect;
    private CfgTier commercial;
    private CfgTranche tranche;
    private StkItem article;
    private StkItem articleAutreTranche;

    @BeforeEach
    void setUp() {
        prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-PSNC-" + UUID.randomUUID());
        prospect.setLastName("Rakoto");
        prospect.setFirstName("Jean");
        tierRepository.save(prospect);

        commercial = new CfgTier();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("COMM-PSNC-" + UUID.randomUUID());
        commercial.setLastName("Commercial Test");
        tierRepository.save(commercial);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-PSNC-" + UUID.randomUUID());
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-PSNC-" + UUID.randomUUID());
        category.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(category);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-PSNC-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-PSNC-" + UUID.randomUUID());
        projectRepository.save(project);

        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-PSNC-" + UUID.randomUUID());
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        CfgTranche autreTranche = new CfgTranche();
        autreTranche.setId(UUID.randomUUID());
        autreTranche.setCode("TR-PSNC-AUTRE-" + UUID.randomUUID());
        autreTranche.setCfgProject(project);
        trancheRepository.save(autreTranche);

        article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("ART-PSNC-" + UUID.randomUUID());
        article.setStkItemType(type);
        article.setStkItemCategory(category);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        articleAutreTranche = new StkItem();
        articleAutreTranche.setId(UUID.randomUUID());
        articleAutreTranche.setCode("ART-PSNC-AUTRE-" + UUID.randomUUID());
        articleAutreTranche.setStkItemType(type);
        articleAutreTranche.setStkItemCategory(category);
        articleAutreTranche.setStkVocation(vocation);
        articleAutreTranche.setStkHierarchy(autreTranche);
        articleAutreTranche.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(articleAutreTranche);
    }

    @Test
    void creerProspection_minimalPayload_createsWithGeneratedCode() throws Exception {
        mockMvc.perform(post("/api/prospections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prospectId\":\"" + prospect.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.prospectId").value(prospect.getId().toString()))
                .andExpect(jsonPath("$.prospectNom").value("Jean Rakoto"))
                .andExpect(jsonPath("$.commercialId").doesNotExist())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern(Pattern.compile("\\d{2}/\\d{5}"))));
    }

    @Test
    void creerProspection_avecCommercialEtTranche_createsFullyQualified() throws Exception {
        String body = "{\"prospectId\":\"" + prospect.getId() + "\",\"commercialId\":\"" + commercial.getId()
                + "\",\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + article.getId()
                + "\",\"notes\":\"Intéressé par un T3\",\"ancienClient\":true}";

        mockMvc.perform(post("/api/prospections").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commercialId").value(commercial.getId().toString()))
                .andExpect(jsonPath("$.trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
                .andExpect(jsonPath("$.notes").value("Intéressé par un T3"))
                .andExpect(jsonPath("$.ancienClient").value(true));
    }

    @Test
    void creerProspection_appearsInFindAll_notInPreProspections() throws Exception {
        String body = "{\"prospectId\":\"" + prospect.getId() + "\"}";
        String response = mockMvc.perform(post("/api/prospections").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(post("/api/prospections").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/prospections")
                        .param("search", id.substring(0, 8)))
                .andExpect(status().isOk());
    }

    @Test
    void creerProspection_prospectIdManquant_returns400() throws Exception {
        mockMvc.perform(post("/api/prospections").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerProspection_prospectIdInconnu_returns400() throws Exception {
        mockMvc.perform(post("/api/prospections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prospectId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerProspection_articleDuneAutreTranche_returns400() throws Exception {
        String body = "{\"prospectId\":\"" + prospect.getId() + "\",\"trancheId\":\"" + tranche.getId()
                + "\",\"articleId\":\"" + articleAutreTranche.getId() + "\"}";

        mockMvc.perform(post("/api/prospections").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerProspection_articleSansTrancheId_returns400() throws Exception {
        String body = "{\"prospectId\":\"" + prospect.getId() + "\",\"articleId\":\"" + article.getId() + "\"}";

        mockMvc.perform(post("/api/prospections").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
