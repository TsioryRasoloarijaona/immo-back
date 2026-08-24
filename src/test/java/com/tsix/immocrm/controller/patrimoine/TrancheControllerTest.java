package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class TrancheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Test
    void findById_returnsDetailWithPenaltiesAndDelays() throws Exception {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-TR-TEST");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-DETAIL-TEST");
        tranche.setDescription("Tranche detail test");
        tranche.setCfgProject(project);
        tranche.setCancellationPenalityPercent(new BigDecimal("10.000000"));
        tranche.setWarrantyDelay(12);
        trancheRepository.save(tranche);

        mockMvc.perform(get("/api/tranches/{id}", tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.projetId").value(project.getId().toString()))
                .andExpect(jsonPath("$.code").value("TR-DETAIL-TEST"))
                .andExpect(jsonPath("$.tauxPenaliteAnnulation").value(10.0))
                .andExpect(jsonPath("$.delaiGarantie").value(12));
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/tranches/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findAll_filterByProjetId() throws Exception {
        CfgProject projetA = new CfgProject();
        projetA.setId(UUID.randomUUID());
        projetA.setCode("PRJ-TR-FLAT-A");
        projectRepository.save(projetA);

        CfgProject projetB = new CfgProject();
        projetB.setId(UUID.randomUUID());
        projetB.setCode("PRJ-TR-FLAT-B");
        projectRepository.save(projetB);

        CfgTranche trancheA = new CfgTranche();
        trancheA.setId(UUID.randomUUID());
        trancheA.setCode("TR-FLAT-A");
        trancheA.setCfgProject(projetA);
        trancheRepository.save(trancheA);

        CfgTranche trancheB = new CfgTranche();
        trancheB.setId(UUID.randomUUID());
        trancheB.setCode("TR-FLAT-B");
        trancheB.setCfgProject(projetB);
        trancheRepository.save(trancheB);

        mockMvc.perform(get("/api/tranches").param("projetId", projetA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + trancheA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + trancheB.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_noFilter_returnsAllTranches() throws Exception {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-FLAT-ALL");
        trancheRepository.save(tranche);

        mockMvc.perform(get("/api/tranches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
