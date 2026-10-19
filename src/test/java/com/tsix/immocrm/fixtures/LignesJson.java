package com.tsix.immocrm.fixtures;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Corps {@code {"lignes":[…]}} du PUT liste des onglets de tranche (FEATURE-patrimoine-tranche-onglets.md §8.2). */
public final class LignesJson {

    private LignesJson() {
    }

    public static String lignes(CorpsJson... lignes) {
        return "{\"lignes\":[" + Arrays.stream(lignes).map(CorpsJson::json).collect(Collectors.joining(",")) + "]}";
    }

    /** Même corps, avec un champ supplémentaire au niveau du corps (à côté de {@code lignes}). */
    public static String lignesAvec(String champ, boolean valeur, CorpsJson... lignes) {
        String corps = lignes(lignes);
        return "{\"" + champ + "\":" + valeur + "," + corps.substring(1);
    }
}
