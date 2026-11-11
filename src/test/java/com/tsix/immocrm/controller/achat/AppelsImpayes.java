package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Appels HTTP du lot A3 (FEATURE-achat-impayes.md §4), noms du fil du spec. */
final class AppelsImpayes {

    static final String TOTAUX = "/api/achat/documents/totaux";
    static final String SOLDES = "/api/achat/fournisseurs/soldes";
    static final String SOLDES_EXPORT = "/api/achat/fournisseurs/soldes/export";

    static final String NATURES = "natures";
    static final String NON_SOLDES = "nonSoldes";
    static final String OK_POUR_PAIEMENT = "okPourPaiement";
    static final String PROJET_IDS = "projetIds";

    private AppelsImpayes() {
    }

    /** {@code params} = paires nom / valeur (un nom répété = filtre multiple). */
    static MockHttpServletRequestBuilder appel(String chemin, RequestPostProcessor qui, String... params) {
        MockHttpServletRequestBuilder requete = get(chemin).with(qui);
        for (int i = 0; i + 1 < params.length; i += 2) {
            requete.param(params[i], params[i + 1]);
        }
        return requete;
    }

    static BigDecimal nombre(String json, String chemin) {
        return new BigDecimal(String.valueOf((Object) JsonPath.read(json, chemin)));
    }

    static List<String> fournisseurs(String json) {
        return JsonPath.read(json, "$.lignes[*].fournisseurId");
    }

    /** Valeur du {@code champ} de la seule ligne (fournisseur, société) du solde. */
    static Object champ(String json, UUID fournisseur, String societeCode, String champ) {
        List<Object> valeurs = JsonPath.read(json, "$.lignes[?(@.fournisseurId == '" + fournisseur
                + "' && @.societeCode == '" + societeCode + "')]." + champ);
        assertThat(valeurs).as("ligne %s / %s, champ %s", fournisseur, societeCode, champ).hasSize(1);
        return valeurs.get(0);
    }

    static BigDecimal montant(String json, UUID fournisseur, String societeCode, String champ) {
        return new BigDecimal(String.valueOf(champ(json, fournisseur, societeCode, champ)));
    }
}
