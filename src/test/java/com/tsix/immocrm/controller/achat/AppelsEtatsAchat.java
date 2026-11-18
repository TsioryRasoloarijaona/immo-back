package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureEtatsAchat;
import com.tsix.immocrm.fixtures.LectureExport;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Appels des états Achat N7a (FEATURE-n7-etats-achat.md « Endpoints »), noms du fil du spec. */
final class AppelsEtatsAchat {

    static final String TVA = "/api/achat/etats/tva-deductible";
    static final String TVA_EXPORT = TVA + "/export";
    static final String AVANCES = "/api/achat/etats/avances";
    static final String AVANCES_EXPORT = AVANCES + "/export";
    static final String RETENUES = "/api/achat/etats/retenues-garantie";
    static final String RETENUES_EXPORT = RETENUES + "/export";

    static final String DU = "du";
    static final String AU = "au";
    static final String FORMAT = "format";
    static final String XLSX = "xlsx";
    static final String FACTURE_ID = "factureId";
    static final String PIECE_ID = "pieceId";

    private AppelsEtatsAchat() {
    }

    /** Requête sans aucun paramètre ajouté ; {@code params} = paires nom / valeur. */
    static MockHttpServletRequestBuilder brut(String route, RequestPostProcessor qui, String... params) {
        MockHttpServletRequestBuilder requete = get(route).with(qui);
        for (int i = 0; i + 1 < params.length; i += 2) {
            requete.param(params[i], params[i + 1]);
        }
        return requete;
    }

    /** Requête sur l'année de fixture ({@code du}, {@code au}), {@code format=xlsx} pour un export. */
    static MockHttpServletRequestBuilder annee(String route, RequestPostProcessor qui, String... params) {
        MockHttpServletRequestBuilder requete = brut(route, qui, params)
                .param(DU, FixtureEtatsAchat.DU).param(AU, FixtureEtatsAchat.AU);
        if (route.endsWith("/export")) {
            requete.param(FORMAT, XLSX);
        }
        return requete;
    }

    static int statut(MockMvc mockMvc, MockHttpServletRequestBuilder requete) throws Exception {
        return LectureExport.executer(mockMvc, requete).getResponse().getStatus();
    }

    /** Lignes de {@code $.lignes} dont le champ d'identifiant vaut {@code id}. */
    static List<Map<String, Object>> lignesDe(String json, String champId, UUID id) {
        return JsonPath.read(json, "$.lignes[?(@." + champId + " == '" + id + "')]");
    }

    /** Valeur numérique d'une ligne (absente = échec). */
    static BigDecimal nombre(Map<String, Object> ligne, String champ) {
        assertThat(ligne).containsKey(champ);
        return new BigDecimal(String.valueOf(ligne.get(champ)));
    }

    static BigDecimal nombre(String json, String chemin) {
        return new BigDecimal(String.valueOf((Object) JsonPath.read(json, chemin)));
    }

    /** Classeur exporté (réponse en flux suivie), statut 200 exigé. */
    static byte[] classeur(MockMvc mockMvc, MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult r = LectureExport.executer(mockMvc, requete);
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return r.getResponse().getContentAsByteArray();
    }
}
