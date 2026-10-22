package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Appels HTTP des quatre routes de FEATURE-tdb-vente-rapports-socle.md §4 et lecture des réponses. */
final class AppelsRapports {

    static final String RACINE = "/api/rapports/";
    static final String VENTE = "vente";

    private final MockMvc mockMvc;

    AppelsRapports(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    static String liste(String module) {
        return RACINE + module;
    }

    static String descripteur(String module, String code) {
        return RACINE + module + "/" + code;
    }

    static String resultat(String module, String code) {
        return descripteur(module, code) + "/resultat";
    }

    static String export(String module, String code) {
        return descripteur(module, code) + "/export";
    }

    int statut(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andReturn().getResponse().getStatus();
    }

    String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    /** Codes de tous les rapports de la liste, groupes confondus, doublons conservés. */
    List<String> codesVisibles(RequestPostProcessor qui) throws Exception {
        return JsonPath.read(corps(get(liste(VENTE)).with(qui)), "$[*].rapports[*].code");
    }

    /** Sections du résultat (200 exigé). */
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
    static Map<String, Object> totaux(Map<String, Object> section) {
        return (Map<String, Object>) section.get("totaux");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Map<String, Object>> sousTotaux(Map<String, Object> section) {
        return (Map<String, Map<String, Object>>) section.get("sousTotaux");
    }

    /** Lignes dont la colonne {@code nom} vaut {@code valeur}. */
    static List<Map<String, Object>> lignesOu(Map<String, Object> section, String nom, Object valeur) {
        return lignes(section).stream().filter(l -> Objects.equals(valeur, l.get(nom))).toList();
    }

    static BigDecimal nombre(Object valeur) {
        return valeur == null ? null : new BigDecimal(String.valueOf(valeur));
    }

    static BigDecimal somme(List<Map<String, Object>> lignes, String nom) {
        return lignes.stream().map(l -> nombre(l.get(nom))).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Noms des colonnes de la section dont la {@code fonctionTotal} vaut {@code fonction}. */
    static List<String> colonnesAvecTotal(Map<String, Object> section, String fonction) {
        List<String> noms = new ArrayList<>();
        for (Map<String, Object> colonne : colonnes(section)) {
            if (fonction.equals(colonne.get("fonctionTotal"))) {
                noms.add((String) colonne.get("nom"));
            }
        }
        return noms;
    }

    /** Export : le classeur part en flux (asynchrone) ; un refus (400/403/404) reste synchrone. */
    MvcResult exporter(MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult resultat = mockMvc.perform(requete).andReturn();
        if (resultat.getRequest().isAsyncStarted()) {
            return mockMvc.perform(asyncDispatch(resultat)).andReturn();
        }
        return resultat;
    }
}
