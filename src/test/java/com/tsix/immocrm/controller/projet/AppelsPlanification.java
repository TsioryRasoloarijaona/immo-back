package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Routes N9b (FEATURE-n9-planification.md « API ») et appels MockMvc ; réponses lues en texte. */
final class AppelsPlanification {

    static final String BASE = "/api/projet/planification";
    static final String MODELES = BASE + "/modeles";
    static final String DETAIL = "$.detail";
    static final String CODE_ABSENTE = "PLANIFICATION_ABSENTE";
    static final String MESSAGE_ABSENTE = "Aucune planification n'est définie pour le projet en cours !";

    private final MockMvc mockMvc;

    AppelsPlanification(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    static String planification(UUID id) {
        return BASE + "/" + id;
    }

    static String modele(UUID id) {
        return MODELES + "/" + id;
    }

    static String taches(UUID planification) {
        return planification(planification) + "/taches";
    }

    static String tache(UUID planification, UUID tache) {
        return taches(planification) + "/" + tache;
    }

    static String intervenants(UUID planification) {
        return planification(planification) + "/intervenants";
    }

    static String intervenant(UUID planification, UUID intervenant) {
        return intervenants(planification) + "/" + intervenant;
    }

    static String dateDebut(UUID planification) {
        return planification(planification) + "/date-debut";
    }

    static String versions(UUID planification) {
        return planification(planification) + "/versions";
    }

    static String planificationDuProjet(UUID projet) {
        return "/api/projet/projets/" + projet + "/planification";
    }

    static String tachesDuLot(UUID lot) {
        return "/api/projet/lots/" + lot + "/taches";
    }

    Reponse lire(String url, RequestPostProcessor qui) throws Exception {
        return appeler(get(url), qui);
    }

    Reponse creer(String url, CorpsJson corps, RequestPostProcessor qui) throws Exception {
        return envoyer(post(url), corps, qui);
    }

    Reponse modifier(String url, CorpsJson corps, RequestPostProcessor qui) throws Exception {
        return envoyer(patch(url), corps, qui);
    }

    Reponse supprimer(String url, RequestPostProcessor qui) throws Exception {
        return appeler(delete(url), qui);
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete, CorpsJson corps, RequestPostProcessor qui) throws Exception {
        return appeler(requete.contentType(MediaType.APPLICATION_JSON).content(corps.json()), qui);
    }

    private Reponse appeler(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }
}
