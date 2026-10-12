package com.tsix.immocrm.domain.audit;

import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;

/** Le JSON {@code Details} d'OMRAN : un tableau de tableaux de chaînes {@code [propriété, ancienne, nouvelle(, code)]}. */
public final class DetailsAudit {

    // Details est une colonne text (ANSI) : un caractère hors page de code y deviendrait « ? » ; échappé en \\uXXXX,
    // il reste lisible par OMRAN comme par nous.
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
            .build();

    private DetailsAudit() {
    }

    public static String serialiser(List<ChangementAudit> changements) {
        String[][] lignes = changements.stream()
                .map(c -> new String[]{c.propriete(), c.ancienneValeur(), c.nouvelleValeur()})
                .toArray(String[][]::new);
        return JSON.writeValueAsString(lignes);
    }

    /** Détail d'une suppression : {@code [[NomCourtDeClasse, "", libellé]]}, comme {@code HistorizeDelete}. */
    public static List<ChangementAudit> suppression(TypeObjetAudit type, String libelle) {
        return List.of(new ChangementAudit(type.nomCourt(), "", libelle == null ? "" : libelle));
    }

    /** Illisible (JSON invalide, ancien format) : liste vide, jamais d'erreur. Le 4ᵉ élément (code ressource) est ignoré. */
    public static List<ChangementAudit> lire(String details) {
        if (details == null || details.isBlank()) {
            return List.of();
        }
        String[][] lignes;
        try {
            lignes = JSON.readValue(details, String[][].class);
        } catch (JacksonException e) {
            return List.of();
        }
        if (lignes == null) {
            return List.of();
        }
        return Arrays.stream(lignes)
                .filter(ligne -> ligne != null && ligne.length >= 3)
                .map(ligne -> new ChangementAudit(ligne[0], ligne[1], ligne[2]))
                .toList();
    }
}
