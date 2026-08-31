package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
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

    @Autowired
    private CfgTrancheRepository trancheRepository;

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
        return newItem(code, folder, null);
    }

    private StkItem newItem(String code, ComFolder folder, CfgTranche tranche) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Lot " + code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setComFolderId(folder.getId());
        item.setStkHierarchy(tranche);
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
                .andExpect(jsonPath("$.content[?(@.id=='" + folder.getId() + "')]").exists());
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
                .andExpect(jsonPath("$.content[?(@.id=='" + withStatut.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withoutStatut.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filterByMultipleStatuts() throws Exception {
        StrStatus statutA = new StrStatus();
        statutA.setId(UUID.randomUUID());
        statutA.setCode("RESERVATION-MULTI-A");
        strStatusRepository.save(statutA);

        StrStatus statutB = new StrStatus();
        statutB.setId(UUID.randomUUID());
        statutB.setCode("RESERVATION-MULTI-B");
        strStatusRepository.save(statutB);

        StrStatus statutC = new StrStatus();
        statutC.setId(UUID.randomUUID());
        statutC.setCode("RESERVATION-MULTI-C");
        strStatusRepository.save(statutC);

        ComFolder withA = new ComFolder();
        withA.setId(UUID.randomUUID());
        withA.setCode("DOS-MULTI-A");
        withA.setStkVocation(vocation);
        withA.setStrStatus(statutA);
        folderRepository.save(withA);

        ComFolder withB = new ComFolder();
        withB.setId(UUID.randomUUID());
        withB.setCode("DOS-MULTI-B");
        withB.setStkVocation(vocation);
        withB.setStrStatus(statutB);
        folderRepository.save(withB);

        ComFolder withC = new ComFolder();
        withC.setId(UUID.randomUUID());
        withC.setCode("DOS-MULTI-C");
        withC.setStkVocation(vocation);
        withC.setStrStatus(statutC);
        folderRepository.save(withC);

        mockMvc.perform(get("/api/dossiers")
                        .param("statut", "RESERVATION-MULTI-A", "RESERVATION-MULTI-B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + withA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withB.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withC.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filterByTrancheId() throws Exception {
        CfgTranche trancheA = new CfgTranche();
        trancheA.setId(UUID.randomUUID());
        trancheA.setCode("TR-DOS-FILTER-A");
        trancheRepository.save(trancheA);

        CfgTranche trancheB = new CfgTranche();
        trancheB.setId(UUID.randomUUID());
        trancheB.setCode("TR-DOS-FILTER-B");
        trancheRepository.save(trancheB);

        ComFolder folderA = new ComFolder();
        folderA.setId(UUID.randomUUID());
        folderA.setCode("DOS-TRANCHE-A");
        folderA.setStkVocation(vocation);
        folderRepository.save(folderA);
        newItem("LOT-TRANCHE-A", folderA, trancheA);

        ComFolder folderB = new ComFolder();
        folderB.setId(UUID.randomUUID());
        folderB.setCode("DOS-TRANCHE-B");
        folderB.setStkVocation(vocation);
        folderRepository.save(folderB);
        newItem("LOT-TRANCHE-B", folderB, trancheB);

        mockMvc.perform(get("/api/dossiers").param("trancheId", trancheA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + folderA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + folderB.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_search_matchesClientNomInAdditionToCode() throws Exception {
        CfgTier client = newTier("CLI-SEARCH-1");
        client.setLastName("Randrianasolo");
        tierRepository.save(client);

        ComFolder matching = new ComFolder();
        matching.setId(UUID.randomUUID());
        matching.setCode("DOS-SEARCH-MATCH");
        matching.setStkVocation(vocation);
        matching.setCfgCustomer(client);
        folderRepository.save(matching);

        ComFolder other = new ComFolder();
        other.setId(UUID.randomUUID());
        other.setCode("DOS-SEARCH-OTHER");
        other.setStkVocation(vocation);
        folderRepository.save(other);

        mockMvc.perform(get("/api/dossiers").param("search", "randrian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + matching.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + other.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_search_stillMatchesCode_forFolderWithoutClient() throws Exception {
        ComFolder noClient = new ComFolder();
        noClient.setId(UUID.randomUUID());
        noClient.setCode("DOS-SEARCH-NOCLIENT");
        noClient.setStkVocation(vocation);
        folderRepository.save(noClient);

        mockMvc.perform(get("/api/dossiers").param("search", "search-nocl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + noClient.getId() + "')]").exists());
    }

    @Test
    void findAll_filterByPeriode_defaultChampDate_usesDocDate() throws Exception {
        ComFolder inRange = new ComFolder();
        inRange.setId(UUID.randomUUID());
        inRange.setCode("DOS-PERIODE-IN");
        inRange.setStkVocation(vocation);
        inRange.setDocDate(java.time.LocalDateTime.of(2026, 3, 15, 0, 0));
        folderRepository.save(inRange);

        ComFolder outOfRange = new ComFolder();
        outOfRange.setId(UUID.randomUUID());
        outOfRange.setCode("DOS-PERIODE-OUT");
        outOfRange.setStkVocation(vocation);
        outOfRange.setDocDate(java.time.LocalDateTime.of(2025, 1, 1, 0, 0));
        folderRepository.save(outOfRange);

        mockMvc.perform(get("/api/dossiers")
                        .param("dateDebut", "2026-01-01T00:00:00")
                        .param("dateFin", "2026-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + inRange.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + outOfRange.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filterByPeriode_champDateConcretisation() throws Exception {
        ComFolder concretiseEnRange = new ComFolder();
        concretiseEnRange.setId(UUID.randomUUID());
        concretiseEnRange.setCode("DOS-PERIODE-CONCR-IN");
        concretiseEnRange.setStkVocation(vocation);
        // docDate volontairement hors plage pour prouver que champDate=CONCRETISATION est bien pris en compte
        concretiseEnRange.setDocDate(java.time.LocalDateTime.of(2020, 1, 1, 0, 0));
        concretiseEnRange.setAdvanceDate(java.time.LocalDateTime.of(2026, 6, 1, 0, 0));
        folderRepository.save(concretiseEnRange);

        ComFolder concretiseHorsRange = new ComFolder();
        concretiseHorsRange.setId(UUID.randomUUID());
        concretiseHorsRange.setCode("DOS-PERIODE-CONCR-OUT");
        concretiseHorsRange.setStkVocation(vocation);
        concretiseHorsRange.setAdvanceDate(java.time.LocalDateTime.of(2019, 6, 1, 0, 0));
        folderRepository.save(concretiseHorsRange);

        mockMvc.perform(get("/api/dossiers")
                        .param("champDate", "CONCRETISATION")
                        .param("dateDebut", "2026-01-01T00:00:00")
                        .param("dateFin", "2026-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + concretiseEnRange.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + concretiseHorsRange.getId() + "')]").doesNotExist());
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
