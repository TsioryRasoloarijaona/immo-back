package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P1a : les nouvelles lectures ne servent pas une entité juridique inactive (R1 et R4 de
 * FEATURE-visibilite-transversale-entites.md : masquée = 404, administrateur compris).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatrimoineFicheVisibiliteTest {

    private static final String CODE_PROJET = "PRJ-P1A-INACTIF";
    private static final String UN_PROJET = "/api/projets/{id}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;

    private AdmUser admin;
    private CfgCompany entiteInactive;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-P1A-VIS");
        entiteInactive = fixture.entite("ENT-P1A-INACTIVE", false);
    }

    @Test
    void defautsEntite_404_entiteInactive() throws Exception {
        mockMvc.perform(get("/api/projets/defauts-entite/{entiteId}", entiteInactive.getId())
                        .with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void zones_404_programmeDUneEntiteInactive() throws Exception {
        CfgProject projet = fixture.projet(CODE_PROJET, entiteInactive);

        mockMvc.perform(get("/api/projets/{id}/zones", projet.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {UN_PROJET,"/api/projets/{id}/tranches"})
    void lecturesParId_404_programmeDUneEntiteInactive(String route) throws Exception {
        CfgProject projet = fixture.projet(CODE_PROJET, entiteInactive);

        mockMvc.perform(get(route, projet.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_404_programmeDUneEntiteInactive() throws Exception {
        CfgProject projet = fixture.projet(CODE_PROJET, entiteInactive);

        mockMvc.perform(patch(UN_PROJET,projet.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Renommé\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_404_programmeDUneEntiteInactive() throws Exception {
        CfgProject projet = fixture.projet(CODE_PROJET, entiteInactive);

        mockMvc.perform(delete(UN_PROJET,projet.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void creationZone_404_programmeDUneEntiteInactive() throws Exception {
        CfgProject projet = fixture.projet(CODE_PROJET, entiteInactive);

        mockMvc.perform(post("/api/projets/{id}/zones", projet.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"Z1\",\"description\":\"Zone\"}"))
                .andExpect(status().isNotFound());
    }
}
