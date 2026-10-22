package com.tsix.immocrm.domain.rapport;

import java.util.List;
import java.util.Map;

/**
 * Section d'un résultat : un tableau (lignes = clé {@link ColonneRapport#nom()} → valeur) et ses graphiques.
 * {@code totaux} : valeurs imposées, qui l'emportent sur le calcul du socle ({@code null} = aucune).
 */
public record SectionRapport(String id, String titre, List<ColonneRapport> colonnes, List<Map<String, Object>> lignes,
                             String groupePar, Map<String, Object> totaux, List<GraphiqueRapport> graphiques) {

    public SectionRapport {
        if (id == null || !id.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException("Id de section invalide : " + id);
        }
        colonnes = colonnes == null ? List.of() : List.copyOf(colonnes);
        lignes = lignes == null ? List.of() : List.copyOf(lignes);
        graphiques = graphiques == null ? List.of() : List.copyOf(graphiques);
    }
}
