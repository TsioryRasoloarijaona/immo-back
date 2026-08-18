package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Test
    void findAll_returnsCreatedProjet() throws Exception {
        CfgProject project = newProject("PRJ-TEST-1", "Projet de test");
        projectRepository.save(project);

        mockMvc.perform(get("/api/projets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + project.getId() + "')]", hasSize(1)));
    }

    @Test
    void findById_returnsDetail() throws Exception {
        CfgProject project = newProject("PRJ-TEST-2", "Projet detail");
        project.setCity("Antananarivo");
        projectRepository.save(project);

        mockMvc.perform(get("/api/projets/{id}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId().toString()))
                .andExpect(jsonPath("$.code").value("PRJ-TEST-2"))
                .andExpect(jsonPath("$.ville").value("Antananarivo"));
    }

    @Test
    void findById_unknownId_returns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/projets/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findTranches_returnsTranchesOfProjet() throws Exception {
        CfgProject project = newProject("PRJ-TEST-3", "Projet avec tranches");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-TEST-1");
        tranche.setDescription("Tranche de test");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        mockMvc.perform(get("/api/projets/{id}/tranches", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + tranche.getId() + "')]", hasSize(1)));
    }

    @Test
    void findTranches_unknownProjetId_returns404() throws Exception {
        mockMvc.perform(get("/api/projets/{id}/tranches", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private static CfgProject newProject(String code, String description) {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode(code);
        project.setDescription(description);
        return project;
    }
}
