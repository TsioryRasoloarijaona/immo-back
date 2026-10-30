package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Appels HTTP de l'écran Historique au nom d'un compte donné. FEATURE-admin-historique.md. */
final class AppelsHistorique {

    static final String LISTE = "/api/administration/historique";
    static final String UTILISATEURS = LISTE + "/utilisateurs";
    static final String EXPORT = LISTE + "/export";
    static final String DATE_DEBUT = "dateDebut";
    static final String DATE_FIN = "dateFin";
    static final String TEXTE = "texte";
    static final String UTILISATEUR = "utilisateur";
    static final String ACTION = "action";
    static final String PAGE = "page";
    static final String SIZE = "size";

    private final MockMvc mockMvc;
    private final RequestPostProcessor compte;

    AppelsHistorique(MockMvc mockMvc, RequestPostProcessor compte) {
        this.mockMvc = mockMvc;
        this.compte = compte;
    }

    /** Filtres bornés à une période, suivis de paires nom / valeur. */
    static Map<String, String> periode(LocalDate debut, LocalDate fin, String... paires) {
        Map<String, String> filtres = new LinkedHashMap<>();
        filtres.put(DATE_DEBUT, debut.toString());
        filtres.put(DATE_FIN, fin.toString());
        for (int i = 0; i < paires.length; i += 2) {
            filtres.put(paires[i], paires[i + 1]);
        }
        return filtres;
    }

    ResultActions appeler(String route, Map<String, String> filtres) throws Exception {
        return mockMvc.perform(requete(route, filtres));
    }

    /** Corps JSON d'un appel qui doit répondre 200. */
    String json(String route, Map<String, String> filtres) throws Exception {
        return appeler(route, filtres).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    List<String> ids(Map<String, String> filtres) throws Exception {
        return JsonPath.read(json(LISTE, filtres), "$.content[*].id");
    }

    long total(Map<String, String> filtres) throws Exception {
        Number total = JsonPath.read(json(LISTE, filtres), "$.totalElements");
        return total.longValue();
    }

    List<String> utilisateurs(Map<String, String> filtres) throws Exception {
        return JsonPath.read(json(UTILISATEURS, filtres), "$");
    }

    /** Réponse d'export à 200, écrite d'un bloc ou en flux. */
    MvcResult export(Map<String, String> filtres) throws Exception {
        MvcResult resultat = mockMvc.perform(requete(EXPORT, filtres)).andReturn();
        if (resultat.getRequest().isAsyncStarted()) {
            resultat = mockMvc.perform(asyncDispatch(resultat)).andReturn();
        }
        status().isOk().match(resultat);
        return resultat;
    }

    /** Lignes de la première feuille, en-tête compris, cellules jointes par {@code |}. */
    List<String> lignesExportees(Map<String, String> filtres) throws Exception {
        byte[] contenu = export(filtres).getResponse().getContentAsByteArray();
        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            return lignes(classeur);
        }
    }

    private static List<String> lignes(Workbook classeur) {
        List<String> lignes = new ArrayList<>();
        for (Row ligne : classeur.getSheetAt(0)) {
            StringJoiner cellules = new StringJoiner("|");
            for (Cell cellule : ligne) {
                cellules.add(cellule.toString());
            }
            lignes.add(cellules.toString());
        }
        return lignes;
    }

    private MockHttpServletRequestBuilder requete(String route, Map<String, String> filtres) {
        MockHttpServletRequestBuilder requete = get(route).with(compte);
        filtres.forEach(requete::param);
        return requete;
    }
}
