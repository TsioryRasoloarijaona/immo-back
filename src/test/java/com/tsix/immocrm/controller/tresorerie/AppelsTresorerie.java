package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.JsonPath;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Appels GET de {@code /api/tresorerie} (FEATURE-n3-recettes-depenses.md « Endpoints »), noms du fil du spec. */
final class AppelsTresorerie {

    static final String DECAISSEMENTS = "/api/tresorerie/decaissements";
    static final String AFFECTATIONS = "/api/tresorerie/affectations";
    static final String RECETTES = "/api/tresorerie/recettes/";
    static final String MODULE = "module";
    static final String PRJ = "PRJ";
    static final String BUY = "BUY";
    static final String SENS = "sens";
    static final String DEBIT = "DEBIT";
    static final String CREDIT = "CREDIT";
    static final String TAILLE = "taille";
    static final String TAILLE_MAX = "200";

    private final MockMvc mockMvc;

    AppelsTresorerie(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    record Reponse(int statut, String corps) {

        List<String> ids() {
            return JsonPath.read(corps, "$.lignes[*].id");
        }

        /** Ligne de {@code lignes[]} d'id donné ; échoue s'il n'y en a pas exactement une. */
        Map<String, Object> ligne(UUID id) {
            List<Map<String, Object>> trouvees = JsonPath.read(corps, "$.lignes[?(@.id == '" + id + "')]");
            assertThat(trouvees).as("ligne %s dans %s", id, corps).hasSize(1);
            return trouvees.get(0);
        }

        BigDecimal nombre(String chemin) {
            Object valeur = JsonPath.read(corps, chemin);
            return new BigDecimal(String.valueOf(valeur));
        }

        Object valeur(String chemin) {
            return JsonPath.read(corps, chemin);
        }
    }

    static BigDecimal nombre(Map<String, Object> ligne, String champ) {
        assertThat(ligne).as("champ %s", champ).containsKey(champ);
        return new BigDecimal(String.valueOf(ligne.get(champ)));
    }

    Reponse lire(RequestPostProcessor qui, String url, String... paramsNomValeur) throws Exception {
        MockHttpServletRequestBuilder requete = get(url).with(qui);
        for (int i = 0; i + 1 < paramsNomValeur.length; i += 2) {
            requete = requete.param(paramsNomValeur[i], paramsNomValeur[i + 1]);
        }
        MockHttpServletResponse r = mockMvc.perform(requete).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** Lecture attendue en 200, taille de page large. */
    Reponse ok(RequestPostProcessor qui, String url, String... paramsNomValeur) throws Exception {
        String[] avecTaille = new String[paramsNomValeur.length + 2];
        System.arraycopy(paramsNomValeur, 0, avecTaille, 0, paramsNomValeur.length);
        avecTaille[paramsNomValeur.length] = TAILLE;
        avecTaille[paramsNomValeur.length + 1] = TAILLE_MAX;
        Reponse r = lire(qui, url, avecTaille);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }
}
