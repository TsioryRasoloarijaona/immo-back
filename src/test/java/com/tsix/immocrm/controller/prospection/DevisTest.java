package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GAP-07 (low priority) — devis on a prospection. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DevisTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Test
    void creerDevis_happyPath_thenListsIt() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-1");
        prospectionRepository.save(prospection);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-DEVIS-1");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-DEVIS-1");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"prix\":300000,\"notes\":\"Offre initiale\"}";

        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.prix").value(300000))
                .andExpect(jsonPath("$.notes").value("Offre initiale"));

        mockMvc.perform(get("/api/prospections/{id}/devis", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.trancheId=='" + tranche.getId() + "')]").exists());
    }

    @Test
    void creerDevis_missingTrancheId_returns400() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-2");
        prospectionRepository.save(prospection);

        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"prix\":100000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDevis_unknownProspection_returns404() throws Exception {
        mockMvc.perform(get("/api/prospections/{id}/devis", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
