package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Lecture des réponses du lot A2 (FEATURE-achat-depenses-projet.md §4.2), noms du fil du spec. */
final class LectureDepenses {

    static final String DEPENSES = "/api/projet/depenses";
    static final String REGLEMENTS = "/api/projet/depenses/reglements";
    static final String PROJET_IDS = "projetIds";
    static final String SIZE = "size";
    static final String TAILLE = "200";

    private LectureDepenses() {
    }

    /** Élément de {@code /depenses} du projet ; échoue s'il n'y en a pas exactement un. */
    static Map<String, Object> projet(String json, UUID projetId) {
        List<Map<String, Object>> elements = JsonPath.read(json, "$[?(@.projetId == '" + projetId + "')]");
        assertThat(elements).as("élément du projet %s", projetId).hasSize(1);
        return elements.get(0);
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> lots(Map<String, Object> projet) {
        Object lots = projet.get("lots");
        assertThat(lots).as("lots[]").isInstanceOf(List.class);
        return (List<Map<String, Object>>) lots;
    }

    /** Ligne de {@code lots[]} ; {@code lotId} nul = ligne « hors lot ». */
    static Map<String, Object> lot(Map<String, Object> projet, UUID lotId) {
        String attendu = lotId == null ? null : lotId.toString();
        List<Map<String, Object>> trouves = lots(projet).stream()
                .filter(l -> Objects.equals(l.get("lotId"), attendu)).toList();
        assertThat(trouves).as("ligne de lot %s", lotId).hasSize(1);
        return trouves.get(0);
    }

    static BigDecimal nombre(Map<String, Object> element, String champ) {
        assertThat(element).as("champ %s", champ).containsKey(champ);
        return new BigDecimal(String.valueOf(element.get(champ)));
    }

    /** Lignes de {@code /reglements} dont le {@code reglementCode} est donné. */
    static List<Map<String, Object>> lignesDuReglement(String json, String reglementCode) {
        return JsonPath.read(json, "$.content[?(@.reglementCode == '" + reglementCode + "')]");
    }

    static List<String> codesReglements(String json) {
        return JsonPath.read(json, "$.content[*].reglementCode");
    }

    static long total(String json) {
        return ((Number) JsonPath.read(json, "$.totalElements")).longValue();
    }
}
