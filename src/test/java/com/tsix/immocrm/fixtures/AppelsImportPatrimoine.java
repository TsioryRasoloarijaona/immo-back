package com.tsix.immocrm.fixtures;

import com.jayway.jsonpath.JsonPath;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * Appels de {@code POST /api/import-patrimoine/{type}} (FEATURE-patrimoine-import-export.md §1.1) et lecture du
 * {@code BilanImportDto} renvoyé (§1.3).
 */
public final class AppelsImportPatrimoine {

    public static final String IMPORT = "/api/import-patrimoine/{type}";
    public static final String MODELE = "/api/import-patrimoine/{type}/modele";
    public static final String ARTICLES = "articles";
    public static final String SOUS_ARTICLES = "sous-articles";
    public static final String FICHIER = "fichier";
    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final String CREATION = "CREATION";
    public static final String MISE_A_JOUR = "MISE_A_JOUR";
    public static final String INCHANGEE = "INCHANGEE";
    public static final String ERREUR = "ERREUR";

    private AppelsImportPatrimoine() {
    }

    /** Requête multipart ; {@code trancheId} ou {@code simulation} nuls = paramètre absent. */
    public static MockMultipartHttpServletRequestBuilder requete(String type, UUID trancheId, Boolean simulation,
                                                                 byte[] fichier, RequestPostProcessor qui) {
        MockMultipartHttpServletRequestBuilder requete = multipart(IMPORT, type);
        requete.file(new MockMultipartFile(FICHIER, "import.xlsx", XLSX, fichier));
        if (trancheId != null) {
            requete.param("trancheId", trancheId.toString());
        }
        if (simulation != null) {
            requete.param("simulation", simulation.toString());
        }
        requete.with(qui);
        return requete;
    }

    public static String corps(MvcResult resultat) throws UnsupportedEncodingException {
        return resultat.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    public static List<Map<String, Object>> lignes(String bilan) {
        return JsonPath.read(bilan, "$.lignes");
    }

    /** Ligne du bilan pour le numéro de ligne Excel (2 = première donnée). */
    public static Map<String, Object> ligne(String bilan, int numero) {
        return lignes(bilan).stream()
                .filter(l -> ((Number) l.get("numero")).intValue() == numero)
                .findFirst()
                .orElseThrow(() -> new AssertionError("pas de ligne " + numero + " dans le bilan : " + bilan));
    }

    public static String resultat(String bilan, int numero) {
        return (String) ligne(bilan, numero).get("resultat");
    }

    public static String message(String bilan, int numero) {
        return String.valueOf(ligne(bilan, numero).get("message"));
    }

    public static int entier(String bilan, String champ) {
        return ((Number) JsonPath.read(bilan, "$." + champ)).intValue();
    }

    public static boolean ecrit(String bilan) {
        return JsonPath.read(bilan, "$.ecrit");
    }
}
