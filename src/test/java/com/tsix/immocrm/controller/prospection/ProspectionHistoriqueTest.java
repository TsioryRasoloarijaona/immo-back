package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/prospections/{id}/historique} — piste d'audit des transitions pré-prospect →
 * prospect → prospection, comblant l'absence de tout horodatage de ces étapes côté Omran (voir
 * javadoc {@code ProspectionService#historique}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionHistoriqueTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Test
    void historique_tracePassageProspectPuisProspection() throws Exception {
        ComProspection pre = new ComProspection();
        pre.setId(UUID.randomUUID());
        pre.setCode("PSN-HIST-1");
        prospectionRepository.save(pre);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-HIST-1");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-HIST-1");
        tranche.setCfgProject(project);
        tranche = trancheRepository.save(tranche);

        // 1) pré-prospect -> prospect.
        mockMvc.perform(post("/api/pre-prospections/{id}/creer-prospect", pre.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenom\":\"Jean\",\"nom\":\"Rakoto\"}"))
                .andExpect(status().isCreated());

        CfgTier commercial = new CfgTier();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("COMM-HIST-1");
        commercial.setLastName("Commercial Hist");
        tierRepository.save(commercial);

        // 2) prospect -> prospection (commercial assigné + tranche obligatoire).
        mockMvc.perform(post("/api/prospections/{id}/transformer", pre.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commercialId\":\"" + commercial.getId() + "\",\"trancheId\":\""
                                + tranche.getId() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/prospections/{id}/historique", pre.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                // Plus récent en premier : la transformation en prospection.
                .andExpect(jsonPath("$.content[0].description")
                        .value(org.hamcrest.Matchers.containsString("Transformée en prospection")))
                .andExpect(jsonPath("$.content[1].description")
                        .value(org.hamcrest.Matchers.containsString("Prospect créé et lié")));
    }

    @Test
    void historique_unknownProspection_returns404() throws Exception {
        mockMvc.perform(get("/api/prospections/{id}/historique", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
