package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.entities.cfg.CfgRelationType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.repositories.cfg.CfgRelationTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GAP-06 — prospect fiche sub-resources. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectSubResourcesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private StkVocationRepository vocationRepository;

    @Autowired
    private StrHistoryRepository historyRepository;

    @Autowired
    private CfgRelationTypeRepository relationTypeRepository;

    private CfgTier newProspect(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setIsProspect(true);
        return tierRepository.save(tier);
    }

    @Test
    void getProspections_returnsOnlyThisProspects() throws Exception {
        CfgTier prospect = newProspect("PROS-SUB-1");
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-SUB-1");
        p.setCfgProspect(prospect);
        prospectionRepository.save(p);

        mockMvc.perform(get("/api/prospects/{id}/prospections", prospect.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + p.getId() + "')]").exists());
    }

    @Test
    void getDossiers_returnsOnlyThisCustomers() throws Exception {
        CfgTier prospect = newProspect("PROS-SUB-2");
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SUB-1");
        vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-SUB-1");
        folder.setStkVocation(vocation);
        folder.setCfgCustomer(prospect);
        folderRepository.save(folder);

        mockMvc.perform(get("/api/prospects/{id}/dossiers", prospect.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + folder.getId() + "')]").exists());
    }

    @Test
    void getHistorique_returnsRowsForThisObjectId() throws Exception {
        CfgTier prospect = newProspect("PROS-SUB-3");
        StrHistory h = new StrHistory();
        h.setId(UUID.randomUUID());
        h.setObjectId(prospect.getId());
        h.setDocDate(LocalDateTime.now());
        h.setDescription("Creation");
        h.setAdmUserDescription("admin");
        historyRepository.save(h);

        mockMvc.perform(get("/api/prospects/{id}/historique", prospect.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + h.getId() + "')].description").value("Creation"));
    }

    @Test
    void contacts_addListAndRemove() throws Exception {
        CfgTier prospect = newProspect("PROS-SUB-4");
        CfgTier contact = newProspect("PROS-SUB-4-CONTACT");
        CfgRelationType relationType = new CfgRelationType();
        relationType.setId(UUID.randomUUID());
        relationType.setCode("CONJOINT-TEST");
        relationType.setDescription("Conjoint");
        relationTypeRepository.save(relationType);

        String body = "{\"contactProspectId\":\"" + contact.getId() + "\",\"relationTypeId\":\"" + relationType.getId() + "\"}";

        String response = mockMvc.perform(post("/api/prospects/{id}/contacts", prospect.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactProspectId").value(contact.getId().toString()))
                .andExpect(jsonPath("$.relationType").value("Conjoint"))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/prospects/{id}/contacts", prospect.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.contactProspectId=='" + contact.getId() + "')]").exists());
    }

    @Test
    void contacts_creationInline_creeNouveauTierEtLien() throws Exception {
        CfgTier prospect = newProspect("PROS-SUB-5");
        CfgRelationType relationType = new CfgRelationType();
        relationType.setId(UUID.randomUUID());
        relationType.setCode("CONJOINT-INLINE");
        relationType.setDescription("Conjoint");
        relationTypeRepository.save(relationType);

        // Flux OMRAN : on ne fournit pas de contactProspectId, mais la personne à créer.
        String body = "{\"nouveauContact\":{\"prenom\":\"Kennedy\",\"nom\":\"CONJOINT\","
                + "\"email\":\"kennedy.conjoint@example.com\"},"
                + "\"relationTypeId\":\"" + relationType.getId() + "\"}";

        mockMvc.perform(post("/api/prospects/{id}/contacts", prospect.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactProspectId").isNotEmpty())
                .andExpect(jsonPath("$.nomContact").value("Kennedy CONJOINT"))
                .andExpect(jsonPath("$.emailContact").value("kennedy.conjoint@example.com"))
                .andExpect(jsonPath("$.relationType").value("Conjoint"));

        mockMvc.perform(get("/api/prospects/{id}/contacts", prospect.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nomContact=='Kennedy CONJOINT')]").exists());
    }

    @Test
    void contacts_niLienNiCreation_returns400() throws Exception {
        CfgTier prospect = newProspect("PROS-SUB-6");
        CfgRelationType relationType = new CfgRelationType();
        relationType.setId(UUID.randomUUID());
        relationType.setCode("REL-400");
        relationType.setDescription("Notaire");
        relationTypeRepository.save(relationType);

        mockMvc.perform(post("/api/prospects/{id}/contacts", prospect.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationTypeId\":\"" + relationType.getId() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProspections_unknownProspect_returns404() throws Exception {
        mockMvc.perform(get("/api/prospects/{id}/prospections", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
