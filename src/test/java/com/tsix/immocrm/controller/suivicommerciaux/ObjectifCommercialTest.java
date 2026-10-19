package com.tsix.immocrm.controller.suivicommerciaux;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET|PUT /api/suivi-commerciaux/objectif} — FEATURE-suivi-commerciaux.md R7, §7.6 : lecture par tout
 * authentifié, écriture par l'Administrateur, entier 1..1000, ligne {@code AppParametre} absente ⇒ 60.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ObjectifCommercialTest {

    private static final String URL = "/api/suivi-commerciaux/objectif";
    private static final String CHAMP = "$.objectifPersonnesJointesJour";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String COMMERCIAL = "Commercial";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviCommerciaux fixture;

    private AdmUser admin;
    private AdmUser commercial;

    @BeforeEach
    void setUp() {
        String prefixe = FixtureSuiviCommerciaux.prefixe("SCO");
        admin = fixture.compte("sco.adm." + prefixe, ADMINISTRATEUR);
        commercial = fixture.compte("sco.com." + prefixe, COMMERCIAL);
    }

    private ResultActions modifier(AdmUser compte, String role, String corps) throws Exception {
        return mockMvc.perform(put(URL).with(connecte(compte, role))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private static String corps(int valeur) {
        return "{\"objectifPersonnesJointesJour\":" + valeur + "}";
    }

    @Test
    void lecture_sansParametreEnregistre_vaut60() throws Exception {
        mockMvc.perform(get(URL).with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CHAMP).value(60));
    }

    @Test
    void modification_parAdministrateur_relueParUnCommercial() throws Exception {
        modifier(admin, ADMINISTRATEUR, corps(45))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CHAMP).value(45));

        mockMvc.perform(get(URL).with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CHAMP).value(45));
    }

    @Test
    void modification_deuxFois_garderLaDerniere() throws Exception {
        modifier(admin, ADMINISTRATEUR, corps(45)).andExpect(status().isOk());
        modifier(admin, ADMINISTRATEUR, corps(70)).andExpect(status().isOk());

        mockMvc.perform(get(URL).with(connecte(admin, ADMINISTRATEUR)))
                .andExpect(jsonPath(CHAMP).value(70));
    }

    @Test
    void modification_parUnCommercial_rend403() throws Exception {
        modifier(commercial, COMMERCIAL, corps(45)).andExpect(status().isForbidden());

        mockMvc.perform(get(URL).with(connecte(commercial, COMMERCIAL)))
                .andExpect(jsonPath(CHAMP).value(60));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 1000})
    void bornesIncluses_acceptees(int valeur) throws Exception {
        modifier(admin, ADMINISTRATEUR, corps(valeur))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CHAMP).value(valeur));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 1001})
    void horsBornes_rend400(int valeur) throws Exception {
        modifier(admin, ADMINISTRATEUR, corps(valeur)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"objectifPersonnesJointesJour\":null}"})
    void valeurAbsente_rend400(String corps) throws Exception {
        modifier(admin, ADMINISTRATEUR, corps).andExpect(status().isBadRequest());
    }
}
