package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-TEST");
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-TEST");
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-TEST");
        stkVocationRepository.save(vocation);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-ART-TEST");
        projectRepository.save(project);

        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-ART-TEST");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);
    }

    private StkItem newItem(String code, UUID comFolderId) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Lot de test " + code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkHierarchy(tranche);
        item.setComFolderId(comFolderId);
        return item;
    }

    @Test
    void findArticles_defaultsToDisponible() throws Exception {
        StkItem disponible = newItem("ART-DISPO", null);
        StkItem occupe = newItem("ART-OCCUPE", UUID.randomUUID());
        stkItemRepository.save(disponible);
        stkItemRepository.save(occupe);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + disponible.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + occupe.getId() + "')]").doesNotExist());
    }

    @Test
    void findArticles_statutOccupe() throws Exception {
        StkItem disponible = newItem("ART-DISPO-2", null);
        StkItem occupe = newItem("ART-OCCUPE-2", UUID.randomUUID());
        stkItemRepository.save(disponible);
        stkItemRepository.save(occupe);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId()).param("statut", "OCCUPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + occupe.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + disponible.getId() + "')]").doesNotExist());
    }

    @Test
    void findArticles_unknownTranche_returns404() throws Exception {
        mockMvc.perform(get("/api/tranches/{id}/articles", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returnsDetailWithSousLots() throws Exception {
        StkItem main = newItem("ART-MAIN", null);
        stkItemRepository.save(main);

        StkItem annexe = newItem("ART-ANNEXE", null);
        annexe.setParent(main);
        stkItemRepository.save(annexe);

        mockMvc.perform(get("/api/articles/{id}", main.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(main.getId().toString()))
                .andExpect(jsonPath("$.statut").value("DISPONIBLE"))
                .andExpect(jsonPath("$.trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.sousLots[?(@.id=='" + annexe.getId() + "')]").exists());
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/articles/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
