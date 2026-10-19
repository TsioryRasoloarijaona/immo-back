package com.tsix.immocrm.fixtures;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Corps JSON plat construit champ par champ : un test retire ou remplace un champ sans recopier tout le corps.
 * Une valeur {@code null} est écrite {@code null} (et non omise) ; {@link #sans} omet le champ.
 */
public final class CorpsJson {

    private final Map<String, Object> champs = new LinkedHashMap<>();

    private CorpsJson() {
    }

    public static CorpsJson corps() {
        return new CorpsJson();
    }

    public CorpsJson avec(String champ, Object valeur) {
        champs.put(champ, valeur);
        return this;
    }

    public CorpsJson sans(String champ) {
        champs.remove(champ);
        return this;
    }

    public String json() {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        champs.forEach((cle, valeur) -> joiner.add('"' + cle + "\":" + valeurJson(valeur)));
        return joiner.toString();
    }

    private static String valeurJson(Object valeur) {
        if (valeur == null) {
            return "null";
        }
        if (valeur instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (valeur instanceof Number || valeur instanceof Boolean) {
            return valeur.toString();
        }
        if (valeur instanceof Collection<?> liste) {
            StringJoiner tableau = new StringJoiner(",", "[", "]");
            liste.forEach(element -> tableau.add(valeurJson(element)));
            return tableau.toString();
        }
        return '"' + valeur.toString().replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
