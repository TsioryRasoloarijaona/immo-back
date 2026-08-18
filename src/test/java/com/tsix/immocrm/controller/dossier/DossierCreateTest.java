package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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

        String body = """
                {"prospectionId":"%s","articleIds":["%s","%s"]}
                """.formatted(prospection.getId(), main.getId(), annexe.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articles.length()").value(2))
                .andExpect(jsonPath("$.clientId").value(prospection.getCfgProspect().getId().toString()));
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
