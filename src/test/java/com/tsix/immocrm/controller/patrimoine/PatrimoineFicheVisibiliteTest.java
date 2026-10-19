package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R4 du programme — FEATURE-patrimoine-tranche-fiche.md §7.1, §7.2, §8.8 : une entité inactive rend son programme
 * introuvable (404) sur toutes les routes par id, administrateur compris.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatrimoineFicheVisibiliteTest {

    private static final String PROJET = "/api/projets/{id}";
    private static final String ZONES = "/api/projets/{id}/zones";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;

    private AdmUser admin;
    private CfgCompany inactive;
    private CfgProject programmeInactif;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1AV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        inactive = fixture.entite("ENT-INACT-" + jeton, false);
        programmeInactif = fixture.projet("PRJ-INACT-" + jeton, inactive);
    }

    // ------------------------------------------------------------------ §7.1

    @Test
    void defautsEntite_404_entiteInactive() throws Exception {
        mockMvc.perform(get("/api/projets/defauts-entite/{entiteId}", inactive.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void defautsEntite_200_entiteActive() throws Exception {
        CfgCompany active = fixture.entite("ENT-ACT-" + jeton, true);

        mockMvc.perform(get("/api/projets/defauts-entite/{entiteId}", active.getId()).with(connecte(admin)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ §7.2 / §8.8

    @Test
    void zones_404_programmeDUneEntiteInactive() throws Exception {
        mockMvc.perform(get(ZONES, programmeInactif.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void zones_creation_404_programmeDUneEntiteInactive() throws Exception {
        mockMvc.perform(post(ZONES, programmeInactif.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("code", "Z-INACT").avec("description", "Zone").json()))
                .andExpect(status().isNotFound());
    }

    @Test
    void zones_modificationEtSuppression_404_programmeDUneEntiteInactive() throws Exception {
        UUID zone = fiche.zone(programmeInactif.getId(), null, "Z-INACT-2", 1);

        mockMvc.perform(patch("/api/projets/{id}/zones/{zoneId}", programmeInactif.getId(), zone).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec("description", "x").json()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/projets/{id}/zones/{zoneId}", programmeInactif.getId(), zone).with(connecte(admin)))
                .andExpect(status().isNotFound());

        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgProjectZone WHERE PKey = :id", zone)).isEqualTo(1);
    }

    @Test
    void detail_404_programmeDUneEntiteInactive() throws Exception {
        mockMvc.perform(get(PROJET, programmeInactif.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void modification_404_programmeDUneEntiteInactive() throws Exception {
        mockMvc.perform(patch(PROJET, programmeInactif.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec("description", "x").json()))
                .andExpect(status().isNotFound());
    }

    @Test
    void suppression_404_programmeDUneEntiteInactive() throws Exception {
        mockMvc.perform(delete(PROJET, programmeInactif.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());

        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgProject WHERE PKey = :id", programmeInactif.getId()))
                .isEqualTo(1);
    }

    @Test
    void tranchesDuProgramme_404_programmeDUneEntiteInactive() throws Exception {
        mockMvc.perform(get("/api/projets/{id}/tranches", programmeInactif.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void detail_200_programmeDUneEntiteActive() throws Exception {
        CfgProject actif = fixture.projet("PRJ-ACT-" + jeton, fixture.entite("ENT-ACT2-" + jeton, true));

        mockMvc.perform(get(PROJET, actif.getId()).with(connecte(admin)))
                .andExpect(status().isOk());
    }
}
