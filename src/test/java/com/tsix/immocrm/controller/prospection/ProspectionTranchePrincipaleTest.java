package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code PATCH /api/prospections/{id}/tranche-principale} et validation croisée sur "autres-tranches". */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionTranchePrincipaleTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

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

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private StkItem articleDeTrancheA;
    private StkItem articleDeTrancheB;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-TP-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-TP-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-TP-TEST");
        stkVocationRepository.save(vocation);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-TP-TEST");
        projectRepository.save(project);

        trancheA = new CfgTranche();
        trancheA.setId(UUID.randomUUID());
        trancheA.setCode("TR-TP-A");
        trancheA.setDescription("Tranche A");
        trancheA.setCfgProject(project);
        trancheRepository.save(trancheA);

        trancheB = new CfgTranche();
        trancheB.setId(UUID.randomUUID());
        trancheB.setCode("TR-TP-B");
        trancheB.setDescription("Tranche B");
        trancheB.setCfgProject(project);
        trancheRepository.save(trancheB);

        articleDeTrancheA = newItem("ART-TP-A", trancheA);
        stkItemRepository.save(articleDeTrancheA);

        articleDeTrancheB = newItem("ART-TP-B", trancheB);
        stkItemRepository.save(articleDeTrancheB);
    }

    private StkItem newItem(String code, CfgTranche tranche) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Lot de test " + code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkHierarchy(tranche);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        return item;
    }

    private ComProspection newProspection() {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-TP-" + UUID.randomUUID());
        return prospectionRepository.save(p);
    }

    @Test
    void modifierTranchePrincipale_happyPath_setTrancheEtArticleCoherents() throws Exception {
        ComProspection prospection = newProspection();

        mockMvc.perform(patch("/api/prospections/{id}/tranche-principale", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + trancheA.getId() + "\",\"articleId\":\""
                                + articleDeTrancheA.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheId").value(trancheA.getId().toString()))
                .andExpect(jsonPath("$.trancheCode").value("TR-TP-A"))
                .andExpect(jsonPath("$.articleId").value(articleDeTrancheA.getId().toString()))
                .andExpect(jsonPath("$.articleCode").value("ART-TP-A"));
    }

    @Test
    void modifierTranchePrincipale_articleDuneAutreTranche_returns400() throws Exception {
        ComProspection prospection = newProspection();

        mockMvc.perform(patch("/api/prospections/{id}/tranche-principale", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + trancheA.getId() + "\",\"articleId\":\""
                                + articleDeTrancheB.getId() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifierTranchePrincipale_changeDeTranche_desassigneArticleIncoherent() throws Exception {
        ComProspection prospection = newProspection();
        prospection.setStkHierarchy(trancheA);
        prospection.setStkItem(articleDeTrancheA);
        prospectionRepository.save(prospection);

        // On bascule vers trancheB sans fournir de nouvel articleId : l'ancien article
        // (appartenant à trancheA) doit être désassigné plutôt que laissé incohérent.
        mockMvc.perform(patch("/api/prospections/{id}/tranche-principale", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + trancheB.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheId").value(trancheB.getId().toString()))
                .andExpect(jsonPath("$.articleId").isEmpty());
    }

    @Test
    void modifierTranchePrincipale_trancheIdManquant_returns400() throws Exception {
        ComProspection prospection = newProspection();

        mockMvc.perform(patch("/api/prospections/{id}/tranche-principale", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifierTranchePrincipale_unknownProspection_returns404() throws Exception {
        mockMvc.perform(patch("/api/prospections/{id}/tranche-principale", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + trancheA.getId() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ajouterAutreTranche_articleDuneAutreTranche_returns400() throws Exception {
        ComProspection prospection = newProspection();

        mockMvc.perform(post("/api/prospections/{id}/autres-tranches", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + trancheA.getId() + "\",\"articleId\":\""
                                + articleDeTrancheB.getId() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ajouterAutreTranche_articleCoherentAvecSaTranche_returnsNomsLisibles() throws Exception {
        ComProspection prospection = newProspection();

        mockMvc.perform(post("/api/prospections/{id}/autres-tranches", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + trancheA.getId() + "\",\"articleId\":\""
                                + articleDeTrancheA.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheDescription").value("Tranche A"))
                .andExpect(jsonPath("$.articleDescription").value("Lot de test ART-TP-A"));
    }
}
