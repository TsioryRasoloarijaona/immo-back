package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TRAVAUX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Appels HTTP de Travaux chantier et Modifications (FEATURE-n2-travaux-chantier.md), noms de champs du spec. */
final class AppelsTravauxChantier {

    record Reponse(int statut, String corps) {
        <T> T lire(String chemin) {
            return JsonPath.read(corps, chemin);
        }
    }

    private final MockMvc mockMvc;
    private final JsonMapper json;
    private final FixtureTravauxChantier fixture;

    AppelsTravauxChantier(MockMvc mockMvc, JsonMapper json, FixtureTravauxChantier fixture) {
        this.mockMvc = mockMvc;
        this.json = json;
        this.fixture = fixture;
    }

    Reponse appeler(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        fixture.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        fixture.flush();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    Reponse envoyer(MockHttpServletRequestBuilder requete, Map<String, Object> corps, RequestPostProcessor qui)
            throws Exception {
        return appeler(requete.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps)), qui);
    }

    /** Corps {@code JourTravailleEcriture} minimal valide : obligatoires de R1 seulement (projet facultatif, nul si absent). */
    static Map<String, Object> ecriture(UUID societe, UUID projet, UUID type, UUID zone, LocalDate date, String code) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("societeId", societe.toString());
        if (projet != null) {
            corps.put("projetId", projet.toString());
        }
        corps.put("typeId", type.toString());
        if (zone != null) {
            corps.put("zoneId", zone.toString());
        }
        corps.put("date", date.toString());
        corps.put("quantite", 3);
        corps.put("quantite1", 2);
        corps.put("code", code);
        return corps;
    }

    Reponse creer(Map<String, Object> corps, RequestPostProcessor qui) throws Exception {
        return envoyer(post(TRAVAUX), corps, qui);
    }

    Reponse modifier(UUID jour, Map<String, Object> corps, RequestPostProcessor qui) throws Exception {
        return envoyer(put(TRAVAUX + "/" + jour), corps, qui);
    }

    /** Ids de {@code GET /travaux-chantier?<filtres>} ; échoue si le statut n'est pas 200. */
    List<String> ids(String filtres, RequestPostProcessor qui) throws Exception {
        Reponse r = appeler(get(TRAVAUX + "?" + filtres), qui);
        if (r.statut() != 200) {
            throw new AssertionError("Liste des travaux : " + r.statut() + " " + r.corps());
        }
        return r.lire("$[*].id");
    }
}
