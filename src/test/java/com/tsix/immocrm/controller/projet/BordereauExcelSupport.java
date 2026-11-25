package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** Import / export Excel du bordereau du lot (FEATURE-lc2-bordereau-excel.md), colonnes OMRAN et noms du fil exacts. */
final class BordereauExcelSupport {

    static final String TYPE_DEFINITIF = "DEFINITIF";
    static final String TYPE_ESTIMATIF = "ESTIMATIF";
    static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    static final List<String> ENTETES = List.of("O.D.T.", "Code", "Description", "Unité", "Quantité", "P.U. HT", "TVA",
            "Remise", "FODEC", "P.U. TTC", "Notes", "Code groupe");
    static final int COL_ODT = 0;
    static final int COL_CODE = 1;
    static final int COL_QUANTITE = 4;
    static final int COL_PU_HT = 5;
    static final int COL_TVA = 6;
    static final int COL_REMISE = 7;
    static final int COL_FODEC = 8;
    static final int COL_PU_TTC = 9;
    static final int COL_CODE_GROUPE = 11;

    private final MockMvc mockMvc;

    BordereauExcelSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** Réponse binaire de l'export ; {@code corps} = le texte (ProblemDetail en cas d'erreur). */
    record Fichier(int statut, byte[] octets, String corps) {
    }

    /** POST /lots/{id}/bordereau/import?type=… (multipart {@code fichier}). */
    Reponse importer(RequestPostProcessor qui, UUID lot, String type, byte[] octets) throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "bordereau.xlsx", MIME_XLSX, octets);
        MockHttpServletResponse r = mockMvc.perform(multipart(AppelsProjet.LOTS + "/" + lot + "/bordereau/import")
                .file(fichier).param("type", type).with(qui)).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** GET /lots/{id}/bordereau/export?type=…&amp;format=…. */
    Fichier exporter(RequestPostProcessor qui, UUID lot, String type, String format) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(get(AppelsProjet.LOTS + "/" + lot + "/bordereau/export")
                .param("type", type).param("format", format).with(qui)).andReturn().getResponse();
        return new Fichier(r.getStatus(), r.getContentAsByteArray(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** Classeur aux en-têtes OMRAN. */
    @SafeVarargs
    static byte[] classeur(List<Object>... lignes) {
        return ClasseurDeTest.xlsx(ENTETES, Arrays.asList(lignes));
    }

    /** Article : description « ART code », unité U, TVA 20, FODEC 0, sans O.D.T., P.U. TTC, notes ni code groupe. */
    static List<Object> article(String code, Object quantite, Object puHT) {
        return ClasseurDeTest.ligne(null, code, "ART " + code, "U", quantite, puHT, 20, null, 0, null, null, null);
    }

    /** Ligne GROUPE : description « GROUPE code ». */
    static List<Object> groupe(String code, String codeGroupe) {
        return ClasseurDeTest.ligne(null, code, "GROUPE " + code, "GROUPE", null, null, null, null, null, null, null,
                codeGroupe);
    }

    /** Copie de la ligne avec {@code valeur} en colonne {@code colonne}. */
    static List<Object> avec(List<Object> ligne, int colonne, Object valeur) {
        List<Object> copie = new ArrayList<>(ligne);
        copie.set(colonne, valeur);
        return copie;
    }

    /** Lignes du bordereau relu dont le code est exactement {@code code}. */
    static List<Map<String, Object>> lignesDeCode(Reponse r, String code) {
        return liste(r, "$.lignes[?(@.code=='" + code + "')]");
    }

    /** La ligne d'id {@code id} (vide si absente). */
    static List<Map<String, Object>> ligneDId(Reponse r, UUID id) {
        return liste(r, "$.lignes[?(@.id=='" + id + "')]");
    }

    /** Ids des groupes de code {@code code}. */
    static List<String> groupesDeCode(Reponse r, String code) {
        return liste(r, "$.groupes[?(@.code=='" + code + "')].id");
    }

    /** Résultat JsonPath ; liste vide si le corps est vide (route absente) ou n'a pas le chemin. */
    static <T> List<T> liste(Reponse r, String chemin) {
        if (r.corps() == null || r.corps().isBlank()) {
            return List.of();
        }
        try {
            return JsonPath.read(r.corps(), chemin);
        } catch (JsonPathException e) {
            return List.of();
        }
    }

    static BigDecimal nb(Map<String, Object> ligne, String champ) {
        Object v = ligne.get(champ);
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }
}
