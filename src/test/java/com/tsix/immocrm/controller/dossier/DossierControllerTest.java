package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
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
class DossierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private StkItemRepository stkItemRepository;

    @Autowired
    private StkItemTypeRepository stkItemTypeRepository;

    @Autowired
    private StkItemCategoryRepository stkItemCategoryRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private StrStatusRepository strStatusRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-DOS-TEST");
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-DOS-TEST");
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-DOS-TEST");
        stkVocationRepository.save(vocation);
    }

    private CfgTier newTier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setLastName("Nom-" + code);
        return tierRepository.save(tier);
    }

    private StkItem newItem(String code, ComFolder folder) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Lot " + code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setComFolderId(folder.getId());
        return stkItemRepository.save(item);
    }

    @Test
    void findAll_returnsCreatedDossier() throws Exception {
        CfgTier customer = newTier("CLI-DOS-1");
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-LIST-1");
        folder.setStkVocation(vocation);
        folder.setCfgCustomer(customer);
        folderRepository.save(folder);

        mockMvc.perform(get("/api/dossiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + folder.getId() + "')]").exists());
    }

    @Test
    void findAll_filterByStatut() throws Exception {
        StrStatus statut = new StrStatus();
        statut.setId(UUID.randomUUID());
        statut.setCode("RESERVATION-TEST");
        strStatusRepository.save(statut);

        ComFolder withStatut = new ComFolder();
        withStatut.setId(UUID.randomUUID());
        withStatut.setCode("DOS-STATUT-1");
        withStatut.setStkVocation(vocation);
        withStatut.setStrStatus(statut);
        folderRepository.save(withStatut);

        ComFolder withoutStatut = new ComFolder();
        withoutStatut.setId(UUID.randomUUID());
        withoutStatut.setCode("DOS-STATUT-2");
        withoutStatut.setStkVocation(vocation);
        folderRepository.save(withoutStatut);

        mockMvc.perform(get("/api/dossiers").param("statut", "RESERVATION-TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + withStatut.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + withoutStatut.getId() + "')]").doesNotExist());
    }

    @Test
    void findById_returnsDetailWithArticlesAndEtapesAtteignables() throws Exception {
        StrStatus statut = new StrStatus();
        statut.setId(UUID.randomUUID());
        statut.setCode("RESERVATION");
        strStatusRepository.save(statut);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-DETAIL-1");
        folder.setStkVocation(vocation);
        folder.setStrStatus(statut);
        folderRepository.save(folder);

        StkItem main = newItem("LOT-MAIN", folder);
        StkItem annexe = newItem("LOT-ANNEXE", folder);

        mockMvc.perform(get("/api/dossiers/{id}", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(folder.getId().toString()))
                .andExpect(jsonPath("$.statut").value("RESERVATION"))
                .andExpect(jsonPath("$.articles.length()").value(2))
                .andExpect(jsonPath("$.articles[?(@.id=='" + main.getId() + "')]").exists())
                .andExpect(jsonPath("$.articles[?(@.id=='" + annexe.getId() + "')]").exists())
                .andExpect(jsonPath("$.etapesAtteignables").isArray())
                .andExpect(jsonPath("$.etapesAtteignables[?(@=='FORMALISE')]").exists())
                .andExpect(jsonPath("$.etapesAtteignables[?(@=='CONCRETISATION')]").exists());
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
