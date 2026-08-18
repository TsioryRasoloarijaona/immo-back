package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PreProspectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Test
    void findAll_returnsUnassignedProspection() throws Exception {
        ComProspection pre = new ComProspection();
        pre.setId(UUID.randomUUID());
        pre.setCode("PRE-LIST-1");
        prospectionRepository.save(pre);

        mockMvc.perform(get("/api/pre-prospections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + pre.getId() + "')]").exists());
    }

    @Test
    void creerProspect_happyPath_linksNewTier() throws Exception {
        ComProspection pre = new ComProspection();
        pre.setId(UUID.randomUUID());
        pre.setCode("PRE-CREATE-1");
        prospectionRepository.save(pre);

        String body = """
                {"prenom":"Nouveau","nom":"Prospect"}""";

        mockMvc.perform(post("/api/pre-prospections/{id}/creer-prospect", pre.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(pre.getId().toString()))
                .andExpect(jsonPath("$.prospectNom").value("Nouveau Prospect"))
                .andExpect(jsonPath("$.prospectId").exists());
    }

    @Test
    void creerProspect_unknownProspection_returns404() throws Exception {
        String body = """
                {"nom":"Test"}""";

        mockMvc.perform(post("/api/pre-prospections/{id}/creer-prospect", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void creerProspect_alreadyLinked_returns400() throws Exception {
        CfgTier existing = new CfgTier();
        existing.setId(UUID.randomUUID());
        existing.setCode("EXISTING-TIER");
        existing.setLastName("Existant");
        tierRepository.save(existing);

        ComProspection already = new ComProspection();
        already.setId(UUID.randomUUID());
        already.setCode("PRE-ALREADY-1");
        already.setCfgProspect(existing);
        prospectionRepository.save(already);

        String body = """
                {"nom":"Autre"}""";

        mockMvc.perform(post("/api/pre-prospections/{id}/creer-prospect", already.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
