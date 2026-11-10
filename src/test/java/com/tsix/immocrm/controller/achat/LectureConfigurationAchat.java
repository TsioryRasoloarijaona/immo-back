package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lecture des deux listes de configuration Achat (FEATURE-achat-regle-commune.md §4.1) et recherche d'une ligne par id. */
final class LectureConfigurationAchat {

    static final String STATUTS = "/api/achat/config/statuts";
    static final String TRANSITIONS = "/api/achat/config/transitions";

    private LectureConfigurationAchat() {
    }

    static List<Map<String, Object>> lire(MockMvc mockMvc, MockHttpServletRequestBuilder requete,
                                          RequestPostProcessor qui) throws Exception {
        String corps = mockMvc.perform(requete.with(qui)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corps, "$");
    }

    static List<Map<String, Object>> lire(MockMvc mockMvc, String url, RequestPostProcessor qui) throws Exception {
        return lire(mockMvc, get(url), qui);
    }

    /** Ligne d'identifiant {@code id} (comparaison sans la casse), {@code null} si absente. */
    static Map<String, Object> ligne(List<Map<String, Object>> lignes, UUID id) {
        return lignes.stream().filter(l -> id.toString().equalsIgnoreCase(Objects.toString(l.get("id"), "")))
                .findFirst().orElse(null);
    }

    /** Rang de chaque id dans la réponse, dans l'ordre des ids donnés (-1 si absent). */
    static List<Integer> rangs(List<Map<String, Object>> lignes, UUID... ids) {
        return java.util.Arrays.stream(ids).map(id -> lignes.indexOf(ligne(lignes, id))).toList();
    }
}
