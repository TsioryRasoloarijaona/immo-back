package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.entities.com.ComProspectionProfile;
import com.tsix.immocrm.entities.com.ComProspectionProfileCategory;
import com.tsix.immocrm.repositories.adm.AdmCityRepository;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginSupportRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReferentielControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionOriginRepository originRepository;

    @Autowired
    private ComProspectionOriginSupportRepository originSupportRepository;

    @Autowired
    private CfgTierTypeRepository tierTypeRepository;

    @Autowired
    private CfgTierKindRepository tierKindRepository;

    @Autowired
    private AdmCountryRepository countryRepository;

    @Autowired
    private AdmCityRepository cityRepository;

    @Autowired
    private ComProspectionProfileCategoryRepository profileCategoryRepository;

    @Autowired
    private ComProspectionProfileRepository profileRepository;

    @Test
    void getEtapesProspection_returnsArray() throws Exception {
        mockMvc.perform(get("/api/referentiels/etapes-prospection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getOriginesProspection_exposesBooleanFlags() throws Exception {
        ComProspectionOrigin origin = new ComProspectionOrigin();
        origin.setId(UUID.randomUUID());
        origin.setCode("ORIG-TEST-1");
        origin.setDescription("Origine de test");
        origin.setIsWithProposer(true);
        origin.setIsWithGift(false);
        origin.setIsWithSupport(true);
        originRepository.save(origin);

        mockMvc.perform(get("/api/referentiels/origines-prospection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + origin.getId() + "')].avecProposeur").value(true))
                .andExpect(jsonPath("$[?(@.id=='" + origin.getId() + "')].avecSupport").value(true));
    }

    @Test
    void getSupportsForOrigine_filtersByOrigineId() throws Exception {
        ComProspectionOrigin origin = new ComProspectionOrigin();
        origin.setId(UUID.randomUUID());
        origin.setCode("ORIG-TEST-2");
        originRepository.save(origin);

        ComProspectionOriginSupport support = new ComProspectionOriginSupport();
        support.setId(UUID.randomUUID());
        support.setCode("SUP-TEST-1");
        support.setComProspectionOrigin(origin);
        originSupportRepository.save(support);

        ComProspectionOrigin otherOrigin = new ComProspectionOrigin();
        otherOrigin.setId(UUID.randomUUID());
        otherOrigin.setCode("ORIG-TEST-3");
        originRepository.save(otherOrigin);

        ComProspectionOriginSupport otherSupport = new ComProspectionOriginSupport();
        otherSupport.setId(UUID.randomUUID());
        otherSupport.setCode("SUP-TEST-2");
        otherSupport.setComProspectionOrigin(otherOrigin);
        originSupportRepository.save(otherSupport);

        mockMvc.perform(get("/api/referentiels/origines-prospection/{origineId}/supports", origin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + support.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + otherSupport.getId() + "')]").doesNotExist());
    }

    @Test
    void getTierKindsForType_filtersByTypeId() throws Exception {
        CfgTierType type = new CfgTierType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-REF-1");
        tierTypeRepository.save(type);

        CfgTierKind kind = new CfgTierKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("KIND-REF-1");
        kind.setCfgTierType(type);
        tierKindRepository.save(kind);

        CfgTierType otherType = new CfgTierType();
        otherType.setId(UUID.randomUUID());
        otherType.setCode("TYPE-REF-2");
        tierTypeRepository.save(otherType);

        CfgTierKind otherKind = new CfgTierKind();
        otherKind.setId(UUID.randomUUID());
        otherKind.setCode("KIND-REF-2");
        otherKind.setCfgTierType(otherType);
        tierKindRepository.save(otherKind);

        mockMvc.perform(get("/api/referentiels/tier-types/{typeId}/kinds", type.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + kind.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + otherKind.getId() + "')]").doesNotExist());
    }

    @Test
    void getPays_parsesNumericPhoneCode() throws Exception {
        AdmCountry country = new AdmCountry();
        country.setId(UUID.randomUUID());
        country.setCode("MG-TEST");
        country.setDescription("Madagascar Test");
        country.setPhoneCode("261");
        countryRepository.save(country);

        mockMvc.perform(get("/api/referentiels/pays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + country.getId() + "')].indicatifTelephone").value(261));
    }

    @Test
    void getVilles_paginatesAndFiltersByPays() throws Exception {
        AdmCountry country = new AdmCountry();
        country.setId(UUID.randomUUID());
        country.setCode("PAYS-VILLE-TEST");
        countryRepository.save(country);

        AdmCity city = new AdmCity();
        city.setId(UUID.randomUUID());
        city.setCode("VILLE-TEST-1");
        city.setDescription("Ville de test");
        city.setAdmCountry(country);
        cityRepository.save(city);

        mockMvc.perform(get("/api/referentiels/villes").param("paysId", country.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[?(@.id=='" + city.getId() + "')]").exists());
    }

    @Test
    void getProfilsProspection_groupsProfilsByCategory() throws Exception {
        ComProspectionProfileCategory category = new ComProspectionProfileCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-PROFIL-TEST");
        category.setDescription("Categorie de test");
        category.setMultiSelect(true);
        profileCategoryRepository.save(category);

        ComProspectionProfile profile = new ComProspectionProfile();
        profile.setId(UUID.randomUUID());
        profile.setCode("PROFIL-TEST-1");
        profile.setComProspectionProfileCategory(category);
        profileRepository.save(profile);

        mockMvc.perform(get("/api/referentiels/profils-prospection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + category.getId() + "')].multiSelect").value(true))
                .andExpect(jsonPath("$[?(@.id=='" + category.getId() + "')].profils[?(@.id=='"
                        + profile.getId() + "')]").exists());
    }
}
