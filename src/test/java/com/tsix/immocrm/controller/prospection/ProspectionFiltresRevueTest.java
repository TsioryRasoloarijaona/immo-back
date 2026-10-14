package com.tsix.immocrm.controller.prospection;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue de FEATURE-prospection-filtres.md §3 : une date-heure ISO-8601 avec décalage (forme de
 * {@code Date.toISOString()}) était acceptée par l'ancien {@code @DateTimeFormat(iso = DATE_TIME)} ;
 * la documentation promet qu'« une date-heure ISO-8601 reste acceptée ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionFiltresRevueTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/api/prospections", "/api/pre-prospections"})
    void dateHeureAvecDecalage_resteAcceptee(String liste) throws Exception {
        mockMvc.perform(get(liste).param("dateDebut", "2026-01-01T00:00:00.000Z")
                        .param("dateFin", "2026-01-31T23:59:59+01:00"))
                .andExpect(status().isOk());
    }
}
