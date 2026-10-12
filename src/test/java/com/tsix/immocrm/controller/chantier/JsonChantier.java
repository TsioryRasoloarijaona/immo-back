package com.tsix.immocrm.controller.chantier;

import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Lecture des réponses JSON : montants comparés en BigDecimal, jamais en double. */
final class JsonChantier {

    private JsonChantier() {
    }

    static BigDecimal montant(Map<String, Object> objet, String champ) {
        Object valeur = objet.get(champ);
        return valeur == null ? null : new BigDecimal(valeur.toString());
    }

    /** L'élément d'id donné dans un tableau JSON ({@code chemin} pointe le tableau, ex. {@code $} ou {@code $.content}). */
    static Map<String, Object> element(String json, String chemin, Object id) {
        List<Map<String, Object>> trouves = JsonPath.read(json, chemin + "[?(@.id=='" + id + "')]");
        if (trouves.size() != 1) {
            throw new AssertionError("élément " + id + " trouvé " + trouves.size() + " fois dans " + chemin);
        }
        return trouves.get(0);
    }
}
