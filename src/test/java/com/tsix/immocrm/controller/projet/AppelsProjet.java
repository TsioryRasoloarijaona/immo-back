package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Appels GET du lot P1 (FEATURE-projet-lots-decomptes.md §2) ; réponses lues en texte, JSON par JsonPath. */
final class AppelsProjet {

    static final String LOTS = "/api/projet/lots";
    static final String DECOMPTES = "/api/projet/decomptes";
    static final String REFERENTIELS = "/api/projet/referentiels";
    static final String TAILLE = "500";

    private final MockMvc mockMvc;

    AppelsProjet(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    record Reponse(int statut, String corps) {

        List<String> ids(String chemin) {
            return JsonPath.read(corps, chemin);
        }

        BigDecimal nombre(String chemin) {
            Object valeur = JsonPath.read(corps, chemin);
            return new BigDecimal(String.valueOf(valeur));
        }

        Object valeur(String chemin) {
            return JsonPath.read(corps, chemin);
        }
    }

    Reponse lire(RequestPostProcessor qui, String url, String... paramsNomValeur) throws Exception {
        MockHttpServletRequestBuilder requete = get(url);
        if (qui != null) {
            requete = requete.with(qui);
        }
        for (int i = 0; i + 1 < paramsNomValeur.length; i += 2) {
            requete = requete.param(paramsNomValeur[i], paramsNomValeur[i + 1]);
        }
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }
}
