package com.tsix.immocrm.controller.equipe;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue FEATURE-reaffecter-filtres.md §2 : ce que la fenêtre « Réaffecter » liste (« Tous mes projets ») doit être
 * réaffectable par le même chef — même sémantique de périmètre que Mon équipe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReaffecterCoherenceTest {

    private static final String CHEF = RolesEquipe.CHEF;

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private EntityManager em;

    private AdmUser chef;
    private AdmUser source;
    private AdmUser destinataire;
    private ComProspection sansTranche;

    @BeforeEach
    void setUp() {
        CfgProject projet = outils.projet("EQ-RCO-A");
        chef = outils.compte("eq.rco.chef", CHEF);
        outils.affecter(chef, projet);
        source = outils.compte("eq.rco.src", RolesEquipe.COMMERCIAL);
        outils.affecter(source, projet);
        destinataire = outils.compte("eq.rco.dst", RolesEquipe.COMMERCIAL);
        outils.affecter(destinataire, projet);

        sansTranche = outils.preProspection("EQ-RCO-SANS-TRANCHE", null, LocalDateTime.now().minusDays(1));
        sansTranche.setCfgCommercial(source.getCfgEmployee());
        em.flush();
    }

    @Test
    void preProspectionListeeDansLaFenetre_estReaffectableParLeMemeChef() throws Exception {
        mockMvc.perform(get("/api/pre-prospections")
                        .with(EquipeOutils.connecte(chef, CHEF))
                        .param("commercial", source.getCfgEmployee().getId().toString())
                        .param("ouvertes", "true")
                        .param("dateDebut", LocalDateTime.now().minusDays(30).toString())
                        .param("sort", "docDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", hasItem("EQ-RCO-SANS-TRANCHE")));

        mockMvc.perform(post("/api/equipe/reaffecter")
                        .with(EquipeOutils.connecte(chef, CHEF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prospectionIds\":[\"" + sansTranche.getId() + "\"],\"commercialId\":\""
                                + destinataire.getCfgEmployee().getId() + "\"}"))
                .andExpect(status().isOk());
    }
}
