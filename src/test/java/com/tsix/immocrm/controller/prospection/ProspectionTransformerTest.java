package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
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

/**
 * {@code POST /api/prospections/{id}/transformer}. Depuis la règle "tranche obligatoire", la
 * transformation affecte le commercial ET garantit qu'une tranche (projet) est rattachée : soit
 * fournie dans le corps ({@code trancheId}), soit déjà présente sur la prospection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionTransformerTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;
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

    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private StkItem articleDeTrancheA;
    private StkItem articleDeTrancheB;

    @BeforeEach
    void setUp() {
        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-TR-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-TR-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-TR-TEST");
        stkVocationRepository.save(vocation);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-TR-TEST");
        projectRepository.save(project);

        trancheA = new CfgTranche();
        trancheA.setId(UUID.randomUUID());
        trancheA.setCode("TR-TRANS-A");
        trancheA.setDescription("Tranche A");
        trancheA.setCfgProject(project);
        // Reprendre l'instance renvoyée : id assigné → save() passe par merge() et rend une autre
        // instance gérée (même raison que ProspectionTranchePrincipaleTest).
        trancheA = trancheRepository.save(trancheA);

        trancheB = new CfgTranche();
        trancheB.setId(UUID.randomUUID());
        trancheB.setCode("TR-TRANS-B");
        trancheB.setDescription("Tranche B");
        trancheB.setCfgProject(project);
        trancheB = trancheRepository.save(trancheB);

        articleDeTrancheA = stkItemRepository.save(newItem("ART-TR-A", trancheA, itemType, itemCategory, vocation));
        articleDeTrancheB = stkItemRepository.save(newItem("ART-TR-B", trancheB, itemType, itemCategory, vocation));
    }

    private StkItem newItem(String code, CfgTranche tranche, StkItemType type, StkItemCategory cat, StkVocation voc) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Lot de test " + code);
        item.setStkItemType(type);
        item.setStkItemCategory(cat);
        item.setStkVocation(voc);
        item.setStkHierarchy(tranche);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        return item;
    }

    private CfgTier newTier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setLastName("Nom-" + code);
        return tierRepository.save(tier);
    }

    private ComProspection newProspectionAvecProspect(String code) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code);
        prospection.setCfgProspect(newTier("PROS-" + code));
        return prospectionRepository.save(prospection);
    }

    @Test
    void transformer_avecTrancheIdDansCorps_affecteCommercialEtTranche() throws Exception {
        CfgTier commercial = newTier("COMM-TRANS-1");
        ComProspection prospection = newProspectionAvecProspect("PSN-TRANS-1");

        String body = "{\"commercialId\":\"" + commercial.getId() + "\",\"trancheId\":\""
                + trancheA.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialId").value(commercial.getId().toString()))
                .andExpect(jsonPath("$.trancheId").value(trancheA.getId().toString()))
                .andExpect(jsonPath("$.trancheCode").value("TR-TRANS-A"));
    }

    @Test
    void transformer_trancheDejaPresente_sansTrancheIdDansCorps() throws Exception {
        CfgTier commercial = newTier("COMM-TRANS-2");
        ComProspection prospection = newProspectionAvecProspect("PSN-TRANS-2");
        prospection.setStkHierarchy(trancheA);
        prospectionRepository.save(prospection);

        String body = "{\"commercialId\":\"" + commercial.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialId").value(commercial.getId().toString()))
                .andExpect(jsonPath("$.trancheId").value(trancheA.getId().toString()));
    }

    @Test
    void transformer_sansAucuneTranche_returns400() throws Exception {
        CfgTier commercial = newTier("COMM-TRANS-3");
        ComProspection prospection = newProspectionAvecProspect("PSN-TRANS-3");

        String body = "{\"commercialId\":\"" + commercial.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transformer_articleDuneAutreTranche_returns400() throws Exception {
        CfgTier commercial = newTier("COMM-TRANS-4");
        ComProspection prospection = newProspectionAvecProspect("PSN-TRANS-4");

        String body = "{\"commercialId\":\"" + commercial.getId() + "\",\"trancheId\":\""
                + trancheA.getId() + "\",\"articleId\":\"" + articleDeTrancheB.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transformer_unknownProspection_returns404() throws Exception {
        String body = "{\"commercialId\":\"" + UUID.randomUUID() + "\",\"trancheId\":\""
                + trancheA.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void transformer_withoutProspect_returns400() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-TRANS-NOPROSPECT");
        prospectionRepository.save(prospection);

        String body = "{\"commercialId\":\"" + UUID.randomUUID() + "\",\"trancheId\":\""
                + trancheA.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transformer_missingCommercialId_returns400() throws Exception {
        ComProspection prospection = newProspectionAvecProspect("PSN-TRANS-5");

        String body = "{\"trancheId\":\"" + trancheA.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
