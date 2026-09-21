package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgRelationType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgRelationTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
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
 * {@code GET/POST /api/prospections/{id}/contacts} — les contacts du prospect de la prospection,
 * pour accompagner l'affichage du détail. Création à la volée de la personne-contact (flux OMRAN).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionContactsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private CfgRelationTypeRepository relationTypeRepository;

    private CfgTier newTier(String code) {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        t.setLastName("Nom-" + code);
        t.setIsProspect(true);
        return tierRepository.save(t);
    }

    private ComProspection newProspection(String code, CfgTier prospect) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setCfgProspect(prospect);
        return prospectionRepository.save(p);
    }

    private CfgRelationType newRelation(String code, String libelle) {
        CfgRelationType rt = new CfgRelationType();
        rt.setId(UUID.randomUUID());
        rt.setCode(code);
        rt.setDescription(libelle);
        return relationTypeRepository.save(rt);
    }

    @Test
    void ajouterEtListerContact_creationInline() throws Exception {
        CfgTier prospect = newTier("PROS-PC-1");
        ComProspection prospection = newProspection("PSN-PC-1", prospect);
        CfgRelationType relation = newRelation("REL-PC-1", "Conjoint");

        String body = "{\"nouveauContact\":{\"prenom\":\"Kennedy\",\"nom\":\"CONJOINT\","
                + "\"email\":\"k.conjoint@example.com\"},"
                + "\"relationTypeId\":\"" + relation.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/contacts", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactProspectId").isNotEmpty())
                .andExpect(jsonPath("$.nomContact").value("Kennedy CONJOINT"))
                .andExpect(jsonPath("$.emailContact").value("k.conjoint@example.com"))
                .andExpect(jsonPath("$.relationType").value("Conjoint"));

        mockMvc.perform(get("/api/prospections/{id}/contacts", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nomContact=='Kennedy CONJOINT')]").exists());
    }

    @Test
    void ajouterContact_lienTierExistant() throws Exception {
        CfgTier prospect = newTier("PROS-PC-2");
        CfgTier contact = newTier("CONTACT-PC-2");
        ComProspection prospection = newProspection("PSN-PC-2", prospect);
        CfgRelationType relation = newRelation("REL-PC-2", "Notaire");

        String body = "{\"contactProspectId\":\"" + contact.getId() + "\",\"relationTypeId\":\""
                + relation.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/contacts", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactProspectId").value(contact.getId().toString()))
                .andExpect(jsonPath("$.relationType").value("Notaire"));
    }

    @Test
    void getContacts_preProspectionSansProspect_listeVide() throws Exception {
        // Prospection sans prospect (pré-prospection) : liste vide, jamais 400.
        ComProspection preProspection = newProspection("PSN-PC-3", null);

        mockMvc.perform(get("/api/prospections/{id}/contacts", preProspection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ajouterContact_prospectionSansProspect_returns400() throws Exception {
        ComProspection preProspection = newProspection("PSN-PC-4", null);
        CfgRelationType relation = newRelation("REL-PC-4", "Conjoint");

        String body = "{\"contactProspectId\":\"" + UUID.randomUUID() + "\",\"relationTypeId\":\""
                + relation.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/contacts", preProspection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getContacts_unknownProspection_returns404() throws Exception {
        mockMvc.perform(get("/api/prospections/{id}/contacts", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
