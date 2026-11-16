package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Routes et corps {@code StatutLotEcritureDto} de FEATURE-restes-projet-achat.md §6 (noms du fil) ; transitions de lot sur le
 * modèle des transitions d'achat ({@code TransitionAchatEcritureDto} : ordre, description, statutDepartId, statutArriveeId).
 */
final class EcritureStatutsLot {

    static final String STATUTS = "/api/projet/config/statuts-lot";
    static final String STATUT = STATUTS + "/{id}";
    static final String CATALOGUE = STATUTS + "/catalogue";
    static final String TRANSITIONS = "/api/projet/config/transitions-lot";
    static final String TRANSITION = TRANSITIONS + "/{id}";

    static final String GENRE_ID = "genreId";
    static final String ORDRE = "ordre";
    static final String CODE = "code";
    static final String DESCRIPTION = "description";
    static final String TYPE_ID = "typeId";
    static final String DUREE = "dureeVieJours";
    static final String COULEUR = "couleur";
    static final String IMAGE_ID = "imageId";
    static final String NOTES = "notes";
    static final String CASES = "cases";
    static final String CHAMPS = "champsObligatoires";
    static final String DEPART = "statutDepartId";
    static final String ARRIVEE = "statutArriveeId";
    static final String ROLES = "roleIds";

    private EcritureStatutsLot() {
    }

    /** Corps minimal valide : genre + les 4 obligatoires (O.D.T, code, description, type). */
    static Map<String, Object> corps(UUID genre, String code, UUID type) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put(GENRE_ID, genre.toString());
        c.put(ORDRE, 3);
        c.put(CODE, code);
        c.put(DESCRIPTION, "Statut " + code);
        c.put(TYPE_ID, type.toString());
        return c;
    }

    /** Corps {@code TransitionLotEcritureDto} (contrat du 16/11) ; {@code code} = description comme en prod, aucun rôle. */
    static Map<String, Object> transition(UUID genre, UUID depart, UUID arrivee, String description, UUID... roles) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put(GENRE_ID, genre.toString());
        c.put(ORDRE, 4);
        c.put(CODE, description);
        c.put(DESCRIPTION, description);
        c.put(DEPART, depart.toString());
        c.put(ARRIVEE, arrivee.toString());
        c.put("avecCommentaire", false);
        c.put("avecMotDePasse", false);
        c.put("transitionAutomatique", false);
        c.put(ROLES, Arrays.stream(roles).map(UUID::toString).toList());
        return c;
    }

    static MockHttpServletRequestBuilder creation(JsonMapper json, String url, Map<String, Object> corps) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps));
    }

    static MockHttpServletRequestBuilder modification(JsonMapper json, String url, Object id, Map<String, Object> corps) {
        return put(url, id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps));
    }

    static MockHttpServletRequestBuilder suppression(String url, Object id) {
        return delete(url, id);
    }

    static UUID id(MvcResult reponse) throws Exception {
        return UUID.fromString(String.valueOf((Object) JsonPath.read(corpsDe(reponse), "$.id")));
    }

    static String detail(MvcResult reponse) throws Exception {
        return String.valueOf((Object) JsonPath.read(corpsDe(reponse), "$.detail"));
    }

    /** Ligne de GET /statuts-lot dont l'id vaut {@code id} (sans casse), {@code null} si absente. */
    static Map<String, Object> ligne(MockMvc mockMvc, RequestPostProcessor qui, UUID id) throws Exception {
        String corps = corpsDe(mockMvc.perform(get(STATUTS).with(qui)).andExpect(status().isOk()).andReturn());
        List<Map<String, Object>> lignes = JsonPath.read(corps, "$");
        return lignes.stream().filter(l -> id.toString().equalsIgnoreCase(String.valueOf(l.get("id"))))
                .findFirst().orElse(null);
    }

    private static String corpsDe(MvcResult reponse) throws Exception {
        return reponse.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
