package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Appels HTTP des routes N6a (FEATURE-n6-demandes-achat.md « Endpoints ») ; réponses lues en texte, JSON par JsonPath. */
final class DemandesAchatAppels {

    static final String RACINE = "/api/achat/demandes-achat";
    static final String IDS = "$[*].id";
    static final String DETAIL = "$.detail";
    static final String TRANSITION_ID = "transitionId";
    static final String NOTES = "notes";
    static final String SECOND_VALIDEUR = "secondValideur";

    private final MockMvc mockMvc;

    DemandesAchatAppels(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    record Reponse(int statut, String corps) {

        List<String> ids(String chemin) {
            return JsonPath.read(corps, chemin);
        }

        Object valeur(String chemin) {
            return JsonPath.read(corps, chemin);
        }
    }

    Reponse lister(RequestPostProcessor qui, String... paramsNomValeur) throws Exception {
        MockHttpServletRequestBuilder requete = get(RACINE).with(qui);
        for (int i = 0; i + 1 < paramsNomValeur.length; i += 2) {
            requete = requete.param(paramsNomValeur[i], paramsNomValeur[i + 1]);
        }
        return envoyer(requete);
    }

    Reponse exporter(RequestPostProcessor qui) throws Exception {
        return envoyer(get(RACINE + "/export").with(qui));
    }

    Reponse fiche(RequestPostProcessor qui, UUID id) throws Exception {
        return envoyer(get(RACINE + "/" + id).with(qui));
    }

    Reponse creer(RequestPostProcessor qui, CorpsJson corps) throws Exception {
        return envoyer(post(RACINE).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    Reponse modifier(RequestPostProcessor qui, UUID id, CorpsJson corps) throws Exception {
        return envoyer(put(RACINE + "/" + id).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    Reponse supprimer(RequestPostProcessor qui, UUID id) throws Exception {
        return envoyer(delete(RACINE + "/" + id).with(qui));
    }

    Reponse transitions(RequestPostProcessor qui, UUID id) throws Exception {
        return envoyer(get(RACINE + "/" + id + "/transitions").with(qui));
    }

    Reponse executer(RequestPostProcessor qui, UUID id, CorpsJson corps) throws Exception {
        return envoyer(post(RACINE + "/" + id + "/transitions").with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(corps.json()));
    }

    Reponse executer(RequestPostProcessor qui, UUID id, UUID transition) throws Exception {
        return executer(qui, id, corps().avec(TRANSITION_ID, transition));
    }

    Reponse historique(RequestPostProcessor qui, UUID id) throws Exception {
        return envoyer(get(RACINE + "/" + id + "/historique").with(qui));
    }

    // ------------------------------------------------------------------ N6b : sous-ressources de la DIA ({@code RACINE/{id}/…})

    Reponse lire(RequestPostProcessor qui, UUID id, String sousChemin) throws Exception {
        return envoyer(get(RACINE + "/" + id + sousChemin).with(qui));
    }

    Reponse poster(RequestPostProcessor qui, UUID id, String sousChemin, CorpsJson corps) throws Exception {
        return envoyer(post(RACINE + "/" + id + sousChemin).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    Reponse remplacer(RequestPostProcessor qui, UUID id, String sousChemin, CorpsJson corps) throws Exception {
        return envoyer(put(RACINE + "/" + id + sousChemin).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    Reponse retirer(RequestPostProcessor qui, UUID id, String sousChemin) throws Exception {
        return envoyer(delete(RACINE + "/" + id + sousChemin).with(qui));
    }

    /** R18 : {@code POST /api/projet/lots/{id}/demandes-achat}. */
    Reponse creerDepuisLot(RequestPostProcessor qui, UUID lot, CorpsJson corps) throws Exception {
        return envoyer(post("/api/projet/lots/" + lot + "/demandes-achat").with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(corps.json()));
    }

    static CorpsJson secondValideur(String login, String motDePasse) {
        return corps().avec("login", login).avec("motDePasse", motDePasse);
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }
}
