package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureReunionsChantier;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.REUNIONS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Appels HTTP des réunions de chantier (FEATURE-n1-reunions-chantier.md), noms de champs du spec. */
final class AppelsReunions {

    record Reponse(int statut, String corps) {
        <T> T lire(String chemin) {
            return JsonPath.read(corps, chemin);
        }

        UUID id() {
            return UUID.fromString(lire("$.id"));
        }
    }

    private final MockMvc mockMvc;
    private final FixtureReunionsChantier fixture;

    AppelsReunions(MockMvc mockMvc, FixtureReunionsChantier fixture) {
        this.mockMvc = mockMvc;
        this.fixture = fixture;
    }

    Reponse appeler(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        fixture.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        fixture.flush();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    Reponse envoyer(MockHttpServletRequestBuilder requete, String json, RequestPostProcessor qui) throws Exception {
        return appeler(requete.contentType(MediaType.APPLICATION_JSON).content(json), qui);
    }

    /** POST /reunions ; {@code reference} et {@code prochaineDate} nulles = absentes du corps. */
    Reponse creer(UUID projet, UUID type, String reference, LocalDate date, LocalDate prochaineDate,
                  RequestPostProcessor qui) throws Exception {
        StringBuilder json = new StringBuilder("{\"projetId\":\"%s\",\"typeId\":\"%s\",\"date\":\"%s\""
                .formatted(projet, type, date));
        if (reference != null) {
            json.append(",\"reference\":\"").append(reference).append('"');
        }
        if (prochaineDate != null) {
            json.append(",\"prochaineDate\":\"").append(prochaineDate).append('"');
        }
        return envoyer(post(REUNIONS), json.append('}').toString(), qui);
    }
}
