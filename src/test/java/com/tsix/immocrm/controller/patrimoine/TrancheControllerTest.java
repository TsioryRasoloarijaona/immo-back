package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
}
