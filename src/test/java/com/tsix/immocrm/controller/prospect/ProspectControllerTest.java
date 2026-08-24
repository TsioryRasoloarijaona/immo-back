package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgTierTypeRepository tierTypeRepository;

    private CfgTier newProspect(String firstName, String lastName, String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setFirstName(firstName);
        tier.setLastName(lastName);
        tier.setIsProspect(true);
        return tier;
    }

    @Test
    void findAll_returnsCreatedProspect() throws Exception {
        CfgTier prospect = newProspect("Jean", "Rakoto", "PROS-TEST-1");
        tierRepository.save(prospect);

        mockMvc.perform(get("/api/prospects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + prospect.getId() + "')]").exists());
    }

    @Test
    void findAll_search_filtersByName() throws Exception {
        CfgTier match = newProspect("Alice", "Randria-UNIQUE", "PROS-SEARCH-1");
        CfgTier noMatch = newProspect("Bob", "Autre", "PROS-SEARCH-2");
        tierRepository.save(match);
        tierRepository.save(noMatch);

        mockMvc.perform(get("/api/prospects").param("search", "UNIQUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + match.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + noMatch.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filterByMultipleTypes() throws Exception {
        CfgTierType typeA = new CfgTierType();
        typeA.setId(UUID.randomUUID());
        typeA.setCode("TYPE-MULTI-A");
        tierTypeRepository.save(typeA);

        CfgTierType typeB = new CfgTierType();
        typeB.setId(UUID.randomUUID());
        typeB.setCode("TYPE-MULTI-B");
        tierTypeRepository.save(typeB);

        CfgTierType typeC = new CfgTierType();
        typeC.setId(UUID.randomUUID());
        typeC.setCode("TYPE-MULTI-C");
        tierTypeRepository.save(typeC);

        CfgTier withA = newProspect("With", "TypeA", "PROS-TYPE-A");
        withA.setCfgTierType(typeA);
        tierRepository.save(withA);

        CfgTier withB = newProspect("With", "TypeB", "PROS-TYPE-B");
        withB.setCfgTierType(typeB);
        tierRepository.save(withB);

        CfgTier withC = newProspect("With", "TypeC", "PROS-TYPE-C");
        withC.setCfgTierType(typeC);
        tierRepository.save(withC);

        mockMvc.perform(get("/api/prospects").param("type", "TYPE-MULTI-A", "TYPE-MULTI-B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + withA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withB.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + withC.getId() + "')]").doesNotExist());
    }

    @Test
    void findById_returnsDetail() throws Exception {
        CfgTier prospect = newProspect("Marie", "Rabe", "PROS-TEST-2");
        prospect.setCity("Antananarivo");
        tierRepository.save(prospect);

        mockMvc.perform(get("/api/prospects/{id}", prospect.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prospect.getId().toString()))
                .andExpect(jsonPath("$.nom").value("Rabe"))
                .andExpect(jsonPath("$.ville").value("Antananarivo"));
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/prospects/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_happyPath_returns201() throws Exception {
        String body = """
                {"prenom":"Luc","nom":"Andria","email":"luc@example.com"}""";

        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prenom").value("Luc"))
                .andExpect(jsonPath("$.nom").value("Andria"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void create_missingNomAndRaisonSociale_returns400() throws Exception {
        String body = """
                {"prenom":"SansNom"}""";

        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_happyPath_appliesPartialChanges() throws Exception {
        CfgTier prospect = newProspect("Old", "Name", "PROS-TEST-3");
        tierRepository.save(prospect);

        String body = """
                {"ville":"Toamasina"}""";

        mockMvc.perform(patch("/api/prospects/{id}", prospect.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ville").value("Toamasina"))
                .andExpect(jsonPath("$.nom").value("Name"));
    }

    @Test
    void update_unknownId_returns404() throws Exception {
        mockMvc.perform(patch("/api/prospects/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}
