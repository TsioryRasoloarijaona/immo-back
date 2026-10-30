package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureTaches;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.BiFunction;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureTaches.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureTaches.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTaches.FICHE;
import static com.tsix.immocrm.fixtures.FixtureTaches.NON_COMMENCEE;
import static com.tsix.immocrm.fixtures.FixtureTaches.RACINE;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUT;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUTS;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUT_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.TEXTE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 8 de FEATURE-admin-taches.md : 403 sans rôle Administrateur sur les sept routes. La relecture Administrateur
 * prouve que le refus tombe sur une route qui existe et que rien n'est écrit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TachesSecuriteTest {

    private static final String REFUS = "Refus";

    enum Route {
        LISTE((id, j) -> get(RACINE)),
        FICHE_TACHE((id, j) -> get(FICHE, id)),
        LISTE_STATUTS((id, j) -> get(STATUTS)),
        CREATION((id, j) -> json(post(RACINE), corps().avec(DESCRIPTION, REFUS + " " + j).json())),
        MODIFICATION((id, j) -> json(patch(FICHE, id), corps().avec(DESCRIPTION, REFUS).json())),
        SUPPRESSION((id, j) -> delete(FICHE, id)),
        PASSAGE_STATUT((id, j) -> json(post(STATUT, id), corps().avec(STATUT_ID, EN_COURS.fil()).json()));

        private final BiFunction<UUID, String, MockHttpServletRequestBuilder> requete;

        Route(BiFunction<UUID, String, MockHttpServletRequestBuilder> requete) {
            this.requete = requete;
        }

        private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, String corps) {
            return requete.contentType(MediaType.APPLICATION_JSON).content(corps);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTaches fixture;

    private AdmUser admin;
    private RequestPostProcessor commercial;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.assurer();
        admin = patrimoine.administrateur();
        commercial = FixtureFaisabiliteWorkflow.avecRole(admin, "Commercial");
        jeton = FixtureReferentielsAdmin.jeton();
    }

    private void seuleLaTacheInitiale(UUID id, String code) throws Exception {
        mockMvc.perform(get(RACINE).with(connecte(admin)).param(TEXTE, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].description").value("Tache " + code))
                .andExpect(jsonPath("$.content[0].statutId").value(NON_COMMENCEE.fil()));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Route.class)
    void r8_403_sansRoleAdministrateur_rienEcrit(Route route) throws Exception {
        String code = "SEC-" + jeton;
        UUID id = fixture.tache(code, NON_COMMENCEE);

        mockMvc.perform(route.requete.apply(id, jeton).with(commercial)).andExpect(status().isForbidden());

        seuleLaTacheInitiale(id, code);
    }

    /** Le contrôle de rôle passe avant la validation du corps : pas de 400 pour un non-Administrateur. */
    @Test
    void r8_creationCorpsInvalide_403_pas400() throws Exception {
        String code = "SEC-" + jeton;
        UUID id = fixture.tache(code, NON_COMMENCEE);

        mockMvc.perform(post(RACINE).with(commercial).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        seuleLaTacheInitiale(id, code);
    }

    /** Le contrôle de rôle passe avant la conversion de l'identifiant. */
    @Test
    void r8_identifiantIllisible_403_pas400() throws Exception {
        String code = "SEC-" + jeton;
        UUID id = fixture.tache(code, NON_COMMENCEE);

        mockMvc.perform(get(RACINE + "/pas-un-uuid").with(commercial)).andExpect(status().isForbidden());

        seuleLaTacheInitiale(id, code);
    }
}
