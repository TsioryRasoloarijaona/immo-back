package com.tsix.immocrm.controller.suiviprevisions;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Appels des quatre routes de l'écran (FEATURE-tdb-vente-previsions.md §4.1) et lecture des sections (DTO du socle). */
final class SuiviPrevisionsSupport {

    static final String RACINE = "/api/suivi-previsions";
    static final String PARAMETRES = RACINE + "/parametres";
    static final String DETAIL = RACINE + "/detail";
    static final String EXPORT = RACINE + "/export";
    static final String N18 = "/api/rapports/vente/ComTdbPrevisionState";

    static final String ANNEE = "annee";
    static final String TRANCHE_ID = "trancheId";
    static final String FRAIS = "frais";
    static final String MOIS = "mois";

    static final String QUANTITES = "quantites";
    static final String MONTANTS = "montants";
    static final String REEL = "reel";
    static final String SCHEMA = "schemaFinancement";

    private final MockMvc mockMvc;

    SuiviPrevisionsSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** {@code GET url} avec {@code trancheId} répétés et des paires nom / valeur. */
    static MockHttpServletRequestBuilder requete(String url, RequestPostProcessor qui, List<CfgTranche> tranches,
                                                 String... parametres) {
        MockHttpServletRequestBuilder r = get(url).with(qui);
        for (CfgTranche t : tranches) {
            r.param(TRANCHE_ID, t.getId().toString());
        }
        for (int i = 0; i + 1 < parametres.length; i += 2) {
            r.param(parametres[i], parametres[i + 1]);
        }
        return r;
    }

    int statut(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andReturn().getResponse().getStatus();
    }

    String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    List<Map<String, Object>> sections(MockHttpServletRequestBuilder requete) throws Exception {
        return JsonPath.read(corps(requete), "$.sections");
    }

    static Map<String, Object> section(List<Map<String, Object>> sections, String id) {
        return sections.stream().filter(s -> id.equals(s.get("id"))).findFirst()
                .orElseThrow(() -> new AssertionError("section absente : " + id));
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> lignes(Map<String, Object> section) {
        return (List<Map<String, Object>>) section.get("lignes");
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> colonnes(Map<String, Object> section) {
        return (List<Map<String, Object>>) section.get("colonnes");
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> graphiques(Map<String, Object> section) {
        return (List<Map<String, Object>>) section.get("graphiques");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> totaux(Map<String, Object> section) {
        return (Map<String, Object>) section.get("totaux");
    }

    static List<String> nomsDesColonnes(Map<String, Object> section) {
        return colonnes(section).stream().map(c -> (String) c.get("nom")).toList();
    }

    static Map<String, Object> colonne(Map<String, Object> section, String nom) {
        return colonnes(section).stream().filter(c -> nom.equals(c.get("nom"))).findFirst()
                .orElseThrow(() -> new AssertionError("colonne absente : " + nom));
    }

    /** Ligne du mois {@code mois} (1–12) : les 12 lignes sont dans l'ordre des mois. */
    static Map<String, Object> mois(Map<String, Object> section, int mois) {
        return lignes(section).get(mois - 1);
    }

    static BigDecimal nombre(Object valeur) {
        return valeur == null ? null : new BigDecimal(String.valueOf(valeur));
    }

    static BigDecimal valeur(Map<String, Object> ligne, String colonne) {
        return nombre(ligne.get(colonne));
    }

    static BigDecimal somme(List<Map<String, Object>> lignes, String nom) {
        return lignes.stream().map(l -> nombre(l.get(nom))).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Somme de toutes les valeurs numériques de toutes les lignes (0 = aucun chiffre). */
    static BigDecimal sommeDesChiffres(Map<String, Object> section) {
        return lignes(section).stream().flatMap(l -> l.values().stream()).filter(Number.class::isInstance)
                .map(SuiviPrevisionsSupport::nombre).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Export : le classeur part en flux (asynchrone) ; un refus reste synchrone. */
    MvcResult exporter(MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult resultat = mockMvc.perform(requete).andReturn();
        if (resultat.getRequest().isAsyncStarted()) {
            return mockMvc.perform(asyncDispatch(resultat)).andReturn();
        }
        return resultat;
    }

    /** Texte de toutes les cellules de toutes les feuilles. */
    static String texteDuClasseur(byte[] contenu) throws IOException {
        StringBuilder texte = new StringBuilder();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            for (Sheet feuille : classeur) {
                for (Row ligne : feuille) {
                    for (Cell cellule : ligne) {
                        texte.append(cellule).append('\n');
                    }
                }
            }
        }
        return texte.toString();
    }
}
