package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionProfile;
import com.tsix.immocrm.entities.com.ComProspectionProfileCategory;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmCityRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GAP-04 — prospection qualification sub-resources: villes/categories/types-bien/profils/autres-tranches. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionQualificationTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private AdmCityRepository cityRepository;

    @Autowired
    private StkItemCategoryRepository itemCategoryRepository;

    @Autowired
    private StkItemTypeRepository itemTypeRepository;

    @Autowired
    private ComProspectionProfileCategoryRepository profileCategoryRepository;

    @Autowired
    private ComProspectionProfileRepository profileRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    private ComProspection newProspection() {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-QUALIF-" + UUID.randomUUID());
        return prospectionRepository.save(p);
    }

    @Test
    void villes_addListAndRemove() throws Exception {
        ComProspection prospection = newProspection();
        AdmCity city = new AdmCity();
        city.setId(UUID.randomUUID());
        city.setCode("VILLE-QUALIF-1");
        city.setDescription("Ville qualif");
        cityRepository.save(city);

        String addBody = mockMvc.perform(post("/api/prospections/{id}/villes", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"villeId\":\"" + city.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.villeId").value(city.getId().toString()))
                .andReturn().getResponse().getContentAsString();
        UUID linkId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(addBody, "$.id").toString());

        mockMvc.perform(get("/api/prospections/{id}/villes", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + linkId + "')]").exists());

        mockMvc.perform(delete("/api/prospections/{id}/villes/{itemId}", prospection.getId(), linkId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/prospections/{id}/villes", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + linkId + "')]").doesNotExist());
    }

    @Test
    void categories_addAndList() throws Exception {
        ComProspection prospection = newProspection();
        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-QUALIF-1");
        category.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(category);

        mockMvc.perform(post("/api/prospections/{id}/categories", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorieId\":\"" + category.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorieId").value(category.getId().toString()));

        mockMvc.perform(get("/api/prospections/{id}/categories", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.categorieId=='" + category.getId() + "')]").exists());
    }

    @Test
    void typesBien_addAndList() throws Exception {
        ComProspection prospection = newProspection();
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-QUALIF-1");
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        mockMvc.perform(post("/api/prospections/{id}/types-bien", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeBienId\":\"" + type.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeBienId").value(type.getId().toString()));

        mockMvc.perform(get("/api/prospections/{id}/types-bien", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.typeBienId=='" + type.getId() + "')]").exists());
    }

    @Test
    void profils_addAndList() throws Exception {
        ComProspection prospection = newProspection();
        ComProspectionProfileCategory category = new ComProspectionProfileCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-PROFIL-QUALIF-1");
        profileCategoryRepository.save(category);

        ComProspectionProfile profile = new ComProspectionProfile();
        profile.setId(UUID.randomUUID());
        profile.setCode("PROFIL-QUALIF-1");
        profile.setComProspectionProfileCategory(category);
        profileRepository.save(profile);

        mockMvc.perform(post("/api/prospections/{id}/profils", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profilId\":\"" + profile.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilId").value(profile.getId().toString()))
                .andExpect(jsonPath("$.categorieId").value(category.getId().toString()));

        mockMvc.perform(get("/api/prospections/{id}/profils", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.profilId=='" + profile.getId() + "')]").exists());
    }

    @Test
    void autresTranches_addAndList() throws Exception {
        ComProspection prospection = newProspection();
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-QUALIF-1");
        trancheRepository.save(tranche);

        mockMvc.perform(post("/api/prospections/{id}/autres-tranches", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + tranche.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheId").value(tranche.getId().toString()));

        mockMvc.perform(get("/api/prospections/{id}/autres-tranches", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.trancheId=='" + tranche.getId() + "')]").exists());
    }

    @Test
    void ajouterVille_unknownProspection_returns404() throws Exception {
        mockMvc.perform(post("/api/prospections/{id}/villes", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"villeId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }
}
