package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODES;
import static com.tsix.immocrm.fixtures.FixtureRoles.DROITS;
import static com.tsix.immocrm.fixtures.FixtureRoles.FICHE;
import static com.tsix.immocrm.fixtures.FixtureRoles.RACINE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Appels HTTP de l'écran Rôles au nom d'un compte donné. FEATURE-admin-roles.md. */
final class AppelsRoles {

    private final MockMvc mockMvc;
    private final RequestPostProcessor compte;

    AppelsRoles(MockMvc mockMvc, RequestPostProcessor compte) {
        this.mockMvc = mockMvc;
        this.compte = compte;
    }

    /** Corps de {@code PUT …/droits} ; une liste qui admet {@code null}. */
    static CorpsJson codes(String... codes) {
        return corps().avec(CODES, Arrays.asList(codes));
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, String corps) {
        return requete.contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(json(post(RACINE), corps.json()).with(compte));
    }

    ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(json(patch(FICHE, id), corps.json()).with(compte));
    }

    ResultActions droits(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(json(put(DROITS, id), corps.json()).with(compte));
    }

    ResultActions droits(UUID id, List<String> codes) throws Exception {
        return droits(id, corps().avec(CODES, codes));
    }

    ResultActions supprimer(UUID id) throws Exception {
        return mockMvc.perform(delete(FICHE, id).with(compte));
    }

    ResultActions fiche(UUID id) throws Exception {
        return mockMvc.perform(get(FICHE, id).with(compte));
    }

    ResultActions lire(String route, String... paires) throws Exception {
        MockHttpServletRequestBuilder requete = get(route).with(compte);
        for (int i = 0; i < paires.length; i += 2) {
            requete.param(paires[i], paires[i + 1]);
        }
        return mockMvc.perform(requete);
    }

    /** Corps d'une lecture qui doit répondre 200. */
    String corpsLu(String route, String... paires) throws Exception {
        return lire(route, paires).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
