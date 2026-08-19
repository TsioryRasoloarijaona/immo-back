package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
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
class ProspectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private ComProspectionKindRepository prospectionKindRepository;

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
