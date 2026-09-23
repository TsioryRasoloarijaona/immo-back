package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private ComProspectionKindRepository prospectionKindRepository;

    @Autowired
    private ComProspectionOriginRepository prospectionOriginRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    private CfgTier newTier(String code, String firstName, String lastName) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setFirstName(firstName);
        tier.setLastName(lastName);
        return tierRepository.save(tier);
    }

    @Test
    void findAll_returnsFullProspectionButNotPreProspection() throws Exception {
        CfgTier prospect = newTier("PROS-P1", "Full", "Prospection");
        CfgTier commercial = newTier("COMM-P1", "Com", "Mercial");

        ComProspection full = new ComProspection();
        full.setId(UUID.randomUUID());
        full.setCode("PSN-FULL-1");
        full.setCfgProspect(prospect);
        full.setCfgCommercial(commercial);
        prospectionRepository.save(full);

        ComProspection pre = new ComProspection();
        pre.setId(UUID.randomUUID());
        pre.setCode("PSN-PRE-1");
        pre.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        prospectionRepository.save(pre);

        mockMvc.perform(get("/api/prospections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + full.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + pre.getId() + "')]").doesNotExist());
    }

    @Test
    void findPreProspections_returnsOnlyUnassignedRows() throws Exception {
        ComProspection pre = new ComProspection();
        pre.setId(UUID.randomUUID());
        pre.setCode("PSN-PRE-2");
        pre.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        prospectionRepository.save(pre);

        CfgTier prospect = newTier("PROS-P2", "Assigned", "One");
        ComProspection full = new ComProspection();
        full.setId(UUID.randomUUID());
        full.setCode("PSN-FULL-2");
        full.setCfgProspect(prospect);
        prospectionRepository.save(full);

        mockMvc.perform(get("/api/pre-prospections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + pre.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + full.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filterByEtape() throws Exception {
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("ETAPE-TEST-1");
        prospectionKindRepository.save(kind);

        CfgTier commercial = newTier("COMM-P3", "Com", "Mercial3");
        ComProspection withKind = new ComProspection();
        withKind.setId(UUID.randomUUID());
        withKind.setCode("PSN-ETAPE-1");
        withKind.setCfgCommercial(commercial);
        withKind.setComProspectionKind(kind);
        prospectionRepository.save(withKind);

        ComProspection withoutKind = new ComProspection();
        withoutKind.setId(UUID.randomUUID());
        withoutKind.setCode("PSN-ETAPE-2");
        withoutKind.setCfgCommercial(commercial);
        prospectionRepository.save(withoutKind);

        mockMvc.perform(get("/api/prospections").param("etape", "ETAPE-TEST-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + withKind.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withoutKind.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filterByMultipleEtapes() throws Exception {
        ComProspectionKind kindA = new ComProspectionKind();
        kindA.setId(UUID.randomUUID());
        kindA.setCode("ETAPE-MULTI-A");
        prospectionKindRepository.save(kindA);

        ComProspectionKind kindB = new ComProspectionKind();
        kindB.setId(UUID.randomUUID());
        kindB.setCode("ETAPE-MULTI-B");
        prospectionKindRepository.save(kindB);

        ComProspectionKind kindC = new ComProspectionKind();
        kindC.setId(UUID.randomUUID());
        kindC.setCode("ETAPE-MULTI-C");
        prospectionKindRepository.save(kindC);

        CfgTier commercial = newTier("COMM-MULTI-ETAPE", "Com", "Mercial");

        ComProspection withA = new ComProspection();
        withA.setId(UUID.randomUUID());
        withA.setCode("PSN-MULTI-ETAPE-A");
        withA.setCfgCommercial(commercial);
        withA.setComProspectionKind(kindA);
        prospectionRepository.save(withA);

        ComProspection withB = new ComProspection();
        withB.setId(UUID.randomUUID());
        withB.setCode("PSN-MULTI-ETAPE-B");
        withB.setCfgCommercial(commercial);
        withB.setComProspectionKind(kindB);
        prospectionRepository.save(withB);

        ComProspection withC = new ComProspection();
        withC.setId(UUID.randomUUID());
        withC.setCode("PSN-MULTI-ETAPE-C");
        withC.setCfgCommercial(commercial);
        withC.setComProspectionKind(kindC);
        prospectionRepository.save(withC);

        mockMvc.perform(get("/api/prospections")
                        .param("etape", "ETAPE-MULTI-A", "ETAPE-MULTI-B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + withA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withB.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withC.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filterByOrigine() throws Exception {
        ComProspectionOrigin origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("ORIGINE-TEST-1");
        prospectionOriginRepository.save(origine);

        CfgTier commercial = newTier("COMM-ORIGINE", "Com", "Mercial");
        ComProspection withOrigine = new ComProspection();
        withOrigine.setId(UUID.randomUUID());
        withOrigine.setCode("PSN-ORIGINE-1");
        withOrigine.setCfgCommercial(commercial);
        withOrigine.setComProspectionOrigin(origine);
        prospectionRepository.save(withOrigine);

        ComProspection withoutOrigine = new ComProspection();
        withoutOrigine.setId(UUID.randomUUID());
        withoutOrigine.setCode("PSN-ORIGINE-2");
        withoutOrigine.setCfgCommercial(commercial);
        prospectionRepository.save(withoutOrigine);

        mockMvc.perform(get("/api/prospections").param("origine", "ORIGINE-TEST-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + withOrigine.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withoutOrigine.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filterByMultipleCommerciaux() throws Exception {
        CfgTier commercialA = newTier("COMM-MULTI-A", "Com", "MercialA");
        CfgTier commercialB = newTier("COMM-MULTI-B", "Com", "MercialB");
        CfgTier commercialC = newTier("COMM-MULTI-C", "Com", "MercialC");

        ComProspection withA = new ComProspection();
        withA.setId(UUID.randomUUID());
        withA.setCode("PSN-COMM-MULTI-A");
        withA.setCfgCommercial(commercialA);
        prospectionRepository.save(withA);

        ComProspection withB = new ComProspection();
        withB.setId(UUID.randomUUID());
        withB.setCode("PSN-COMM-MULTI-B");
        withB.setCfgCommercial(commercialB);
        prospectionRepository.save(withB);

        ComProspection withC = new ComProspection();
        withC.setId(UUID.randomUUID());
        withC.setCode("PSN-COMM-MULTI-C");
        withC.setCfgCommercial(commercialC);
        prospectionRepository.save(withC);

        mockMvc.perform(get("/api/prospections")
                        .param("commercial", commercialA.getId().toString(), commercialB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + withA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withB.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withC.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filterByTrancheId_andDtoExposesTrancheId() throws Exception {
        CfgTranche trancheA = new CfgTranche();
        trancheA.setId(UUID.randomUUID());
        trancheA.setCode("TR-PSN-FILTER-A");
        trancheRepository.save(trancheA);

        CfgTranche trancheB = new CfgTranche();
        trancheB.setId(UUID.randomUUID());
        trancheB.setCode("TR-PSN-FILTER-B");
        trancheRepository.save(trancheB);

        CfgTier commercial = newTier("COMM-TRANCHE", "Com", "Mercial");

        ComProspection withA = new ComProspection();
        withA.setId(UUID.randomUUID());
        withA.setCode("PSN-TRANCHE-A");
        withA.setCfgCommercial(commercial);
        withA.setStkHierarchy(trancheA);
        prospectionRepository.save(withA);

        ComProspection withB = new ComProspection();
        withB.setId(UUID.randomUUID());
        withB.setCode("PSN-TRANCHE-B");
        withB.setCfgCommercial(commercial);
        withB.setStkHierarchy(trancheB);
        prospectionRepository.save(withB);

        mockMvc.perform(get("/api/prospections").param("trancheId", trancheA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + withA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withB.getId() + "')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id=='" + withA.getId() + "')].trancheId")
                        .value(trancheA.getId().toString()));
    }

    @Test
    void findAll_filterByPeriode_onDocDate() throws Exception {
        CfgTier commercial = newTier("COMM-PERIODE", "Com", "Mercial");

        ComProspection inRange = new ComProspection();
        inRange.setId(UUID.randomUUID());
        inRange.setCode("PSN-PERIODE-IN");
        inRange.setCfgCommercial(commercial);
        inRange.setDocDate(LocalDateTime.of(2026, 3, 15, 0, 0));
        prospectionRepository.save(inRange);

        ComProspection outOfRange = new ComProspection();
        outOfRange.setId(UUID.randomUUID());
        outOfRange.setCode("PSN-PERIODE-OUT");
        outOfRange.setCfgCommercial(commercial);
        outOfRange.setDocDate(LocalDateTime.of(2025, 1, 1, 0, 0));
        prospectionRepository.save(outOfRange);

        mockMvc.perform(get("/api/prospections")
                        .param("dateDebut", "2026-01-01T00:00:00")
                        .param("dateFin", "2026-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + inRange.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + outOfRange.getId() + "')]").doesNotExist());
    }

    @Test
    void findById_returnsDetail() throws Exception {
        CfgTier prospect = newTier("PROS-P4", "Detail", "Test");
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-DETAIL-1");
        p.setCfgProspect(prospect);
        p.setNotes("Notes de test");
        prospectionRepository.save(p);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId().toString()))
                .andExpect(jsonPath("$.prospectId").value(prospect.getId().toString()))
                .andExpect(jsonPath("$.notes").value("Notes de test"));
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/prospections/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
