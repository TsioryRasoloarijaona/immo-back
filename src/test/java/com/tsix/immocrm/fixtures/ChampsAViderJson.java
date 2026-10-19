package com.tsix.immocrm.fixtures;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Ajoute {@code champsAVider: string[]} (FEATURE-patrimoine-tranche-fiche.md §1.1) à un corps {@link CorpsJson}. */
public final class ChampsAViderJson {

    private ChampsAViderJson() {
    }

    public static String avecChampsAVider(CorpsJson corps, String... noms) {
        String liste = Arrays.stream(noms).map(n -> '"' + n + '"').collect(Collectors.joining(",", "[", "]"));
        String json = corps.json();
        String debut = json.substring(0, json.length() - 1);
        String separateur = "{".equals(debut) ? "" : ",";
        return debut + separateur + "\"champsAVider\":" + liste + "}";
    }
}
