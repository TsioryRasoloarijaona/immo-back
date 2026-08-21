package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
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
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[?(@.id=='" + project.getId() + "')]", hasSize(1)));
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
                .andExpect(jsonPath("$.content[?(@.id=='" + tranche.getId() + "')]", hasSize(1)));
    }

    @Test
    void findTranches_unknownProjetId_returns404() throws Exception {
        mockMvc.perform(get("/api/projets/{id}/tranches", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_defaultsToPageSize10() throws Exception {
        mockMvc.perform(get("/api/projets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(10))));
    }

    @Test
    void findAll_paginatesAcrossTwoPages() throws Exception {
        for (int i = 0; i < 12; i++) {
            projectRepository.save(newProject("PRJ-PAGE-" + i, "Projet pagination " + i));
        }

        mockMvc.perform(get("/api/projets").param("size", "5").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.totalPages").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));

        mockMvc.perform(get("/api/projets").param("size", "5").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content", hasSize(5)));
    }

    private static CfgProject newProject(String code, String description) {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode(code);
        project.setDescription(description);
        return project;
    }
}
