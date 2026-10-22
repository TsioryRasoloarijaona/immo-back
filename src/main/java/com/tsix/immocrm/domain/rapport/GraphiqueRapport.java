package com.tsix.immocrm.domain.rapport;

import java.util.List;

/** Graphique d'une section : catégories lues dans {@code colonneCategorie}, une série par nom de {@code colonnesValeurs}. */
public record GraphiqueRapport(TypeGraphique type, String titre, String colonneCategorie,
                               List<String> colonnesValeurs) {

    public GraphiqueRapport {
        colonnesValeurs = List.copyOf(colonnesValeurs);
    }
}
