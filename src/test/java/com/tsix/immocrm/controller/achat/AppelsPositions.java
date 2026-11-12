package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Appels HTTP du lot A3 « positions fournisseurs » (FEATURE-achat-impayes-soldes.md §5), noms du fil du spec. */
final class AppelsPositions {

    static final String POSITIONS = "/api/achat/fournisseurs/positions";
    static final String POSITIONS_EXPORT = "/api/achat/fournisseurs/positions/export";
    static final String DETAIL = "/api/achat/fournisseurs/{fournisseurId}/position";

    static final String PROJET_IDS = "projetIds";
    static final String SOCIETE_IDS = "societeIds";
    static final String SOCIETE_ID = "societeId";
    static final String DU = "du";
    static final String AU = "au";
    static final String SEARCH = "search";
    static final String MASQUER = "masquerResteZero";

    static final String SOLDE_DEBUT = "soldeDebut";
    static final String NET_A_PAYER = "netAPayer";
    static final String REGLE = "regle";
    static final String SOLDE_FIN = "soldeFin";

    private AppelsPositions() {
    }

    /** {@code params} = paires nom / valeur (un nom répété = filtre multiple). */
    static MockHttpServletRequestBuilder positions(RequestPostProcessor qui, String... params) {
        return avecParams(get(POSITIONS).with(qui), params);
    }

    /** Lignes de solde du fournisseur ; {@code params} = paires nom / valeur. */
    static MockHttpServletRequestBuilder detail(RequestPostProcessor qui, UUID fournisseur, String... params) {
        return avecParams(get(DETAIL, fournisseur).with(qui), params);
    }

    static MockHttpServletRequestBuilder avecParams(MockHttpServletRequestBuilder requete, String... params) {
        for (int i = 0; i + 1 < params.length; i += 2) {
            requete.param(params[i], params[i + 1]);
        }
        return requete;
    }

    /** Lignes (fournisseur, société) de la réponse. */
    static List<Map<String, Object>> lignesDe(String json, UUID fournisseur, UUID societe) {
        return JsonPath.read(json, "$.lignes[?(@.fournisseurId == '" + fournisseur + "' && @.societeId == '"
                + societe + "')]");
    }

    /** Montant {@code champ} de la seule ligne (fournisseur, société). */
    static BigDecimal montant(String json, UUID fournisseur, UUID societe, String champ) {
        List<Map<String, Object>> lignes = lignesDe(json, fournisseur, societe);
        assertThat(lignes).as("ligne %s / %s", fournisseur, societe).hasSize(1);
        return nombre(lignes.get(0).get(champ));
    }

    static Object champ(String json, UUID fournisseur, UUID societe, String champ) {
        List<Map<String, Object>> lignes = lignesDe(json, fournisseur, societe);
        assertThat(lignes).as("ligne %s / %s", fournisseur, societe).hasSize(1);
        return lignes.get(0).get(champ);
    }

    static BigDecimal total(String json, String champ) {
        return nombre(JsonPath.read(json, "$.total." + champ));
    }

    static List<String> fournisseurs(String json) {
        return JsonPath.read(json, "$.lignes[*].fournisseurId");
    }

    static BigDecimal nombre(Object valeur) {
        return new BigDecimal(String.valueOf(valeur));
    }
}
