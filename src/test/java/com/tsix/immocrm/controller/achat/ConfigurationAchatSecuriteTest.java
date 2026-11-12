package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.STATUTS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.TRANSITIONS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès aux listes de configuration Achat — FEATURE-achat-regle-commune.md §4.1 et décision n° 4 : tout compte connecté lit,
 * sans jeton → 401, aucune écriture (POST / PUT / DELETE → 405).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfigurationAchatSecuriteTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente dossiers;

    private RequestPostProcessor commercial;

    @BeforeEach
    void setUp() {
        AdmUser compte = dossiers.compte("A4SEC");
        commercial = FixtureDossierVente.connecte(compte, "Commercial");
    }

    /** Sans jeton → 401. */
    @ParameterizedTest
    @ValueSource(strings = {STATUTS, TRANSITIONS})
    void sansJeton_401(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    /** Décision n° 4 : un compte connecté non administrateur lit les deux listes. */
    @ParameterizedTest
    @ValueSource(strings = {STATUTS, TRANSITIONS})
    void compteConnecteNonAdministrateur_200(String url) throws Exception {
        mockMvc.perform(get(url).with(commercial)).andExpect(status().isOk());
    }

    /**
     * §4.1 « Aucun POST/PUT/DELETE » : la route existe en lecture seule → 405. POST /statuts et POST /transitions sont
     * ouverts par FEATURE-statuts-achat-parite.md (PUT / DELETE y prennent un {id}, la collection reste en 405 ; écritures
     * testées dans StatutsAchat*Test et TransitionsAchatEcritureTest).
     */
    @ParameterizedTest
    @ValueSource(strings = {"PUT " + STATUTS, "DELETE " + STATUTS, "PUT " + TRANSITIONS, "DELETE " + TRANSITIONS})
    void ecriture_405(String methodeEtUrl) throws Exception {
        String[] m = methodeEtUrl.split(" ");

        mockMvc.perform(request(HttpMethod.valueOf(m[0]), m[1]).with(commercial)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
