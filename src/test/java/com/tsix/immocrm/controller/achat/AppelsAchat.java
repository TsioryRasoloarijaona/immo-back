package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.LectureExport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Appels HTTP du lot A1 (FEATURE-achat-lecture-factures.md §3), noms du fil du spec. */
final class AppelsAchat {

    static final String REFERENTIELS = "/api/achat/referentiels";
    static final String DOCUMENTS = "/api/achat/documents";
    static final String DOCUMENT = "/api/achat/documents/{id}";
    static final String EXPORT = "/api/achat/documents/export";

    private AppelsAchat() {
    }

    /** Liste en une page large ; {@code params} = paires nom / valeur (un nom répété = filtre multiple). */
    static MockHttpServletRequestBuilder liste(RequestPostProcessor qui, String... params) {
        MockHttpServletRequestBuilder requete = get(DOCUMENTS).param("size", "200").with(qui);
        for (int i = 0; i + 1 < params.length; i += 2) {
            requete.param(params[i], params[i + 1]);
        }
        return requete;
    }

    static String corps(MockMvc mockMvc, MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult r = mockMvc.perform(requete).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return r.getResponse().getContentAsString();
    }

    static List<String> codes(String json) {
        return JsonPath.read(json, "$.content[*].code");
    }

    static long total(String json) {
        return ((Number) JsonPath.read(json, "$.totalElements")).longValue();
    }

    static BigDecimal montant(String json, String chemin) {
        Object valeur = JsonPath.read(json, chemin);
        return new BigDecimal(String.valueOf(valeur));
    }

    /** Valeur du {@code champ} de la seule ligne de la liste dont le {@code code} est donné ({@code null} possible). */
    static Object champ(String json, String code, String champ) {
        List<Object> valeurs = JsonPath.read(json, "$.content[?(@.code == '" + code + "')]." + champ);
        assertThat(valeurs).as("ligne %s, champ %s", code, champ).hasSize(1);
        return valeurs.get(0);
    }

    static BigDecimal montant(String json, String code, String champ) {
        return new BigDecimal(String.valueOf(champ(json, code, champ)));
    }

    /** Toutes les cellules texte de toutes les feuilles du classeur exporté (réponse en flux ou non). */
    static List<String> textesExport(MockMvc mockMvc, MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult r = LectureExport.executer(mockMvc, requete);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        List<String> textes = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(r.getResponse().getContentAsByteArray()))) {
            for (Sheet feuille : classeur) {
                for (Row ligne : feuille) {
                    for (Cell cellule : ligne) {
                        if (cellule.getCellType() == CellType.STRING) {
                            textes.add(cellule.getStringCellValue());
                        }
                    }
                }
            }
        }
        return textes;
    }
}
