package com.tsix.immocrm.controller.prospection;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class PreProspectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Test
    void findAll_filterByTrancheId_appliesToPreProspectionsToo() throws Exception {
        CfgTranche trancheA = new CfgTranche();
        trancheA.setId(UUID.randomUUID());
        trancheA.setCode("TR-PRE-FILTER-A");
        trancheRepository.save(trancheA);

        CfgTranche trancheB = new CfgTranche();
        trancheB.setId(UUID.randomUUID());
        trancheB.setCode("TR-PRE-FILTER-B");
        trancheRepository.save(trancheB);

        ComProspection preA = new ComProspection();
        preA.setId(UUID.randomUUID());
        preA.setCode("PRE-TRANCHE-A");
        preA.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        preA.setStkHierarchy(trancheA);
        prospectionRepository.save(preA);

        ComProspection preB = new ComProspection();
        preB.setId(UUID.randomUUID());
        preB.setCode("PRE-TRANCHE-B");
        preB.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        preB.setStkHierarchy(trancheB);
        prospectionRepository.save(preB);

        mockMvc.perform(get("/api/pre-prospections").param("trancheId", trancheA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + preA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + preB.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_returnsUnassignedProspection() throws Exception {
        ComProspection pre = new ComProspection();
        pre.setId(UUID.randomUUID());
        pre.setCode("PRE-LIST-1");
        pre.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        prospectionRepository.save(pre);

        mockMvc.perform(get("/api/pre-prospections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + pre.getId() + "')]").exists());
    }

    @Test
    void findAll_and_findById_exposeTempFieldsForUnqualifiedLead() throws Exception {
        ComProspection pre = new ComProspection();
        pre.setId(UUID.randomUUID());
        pre.setCode("PRE-TEMP-1");
        pre.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        pre.setTempDescription("Jean Doe (lead brut)");
        pre.setTempTel("0341234567");
        pre.setTempEMail("jean.doe@example.com");
        prospectionRepository.save(pre);

        String listJson = mockMvc.perform(get("/api/pre-prospections"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        net.minidev.json.JSONArray matches = JsonPath.parse(listJson)
                .read("$.content[?(@.id=='" + pre.getId() + "')].tempDescription");
        assertThat(matches).containsExactly("Jean Doe (lead brut)");

        mockMvc.perform(get("/api/prospections/{id}", pre.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempDescription").value("Jean Doe (lead brut)"))
                .andExpect(jsonPath("$.tempTel").value("0341234567"))
                .andExpect(jsonPath("$.tempEmail").value("jean.doe@example.com"));
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
