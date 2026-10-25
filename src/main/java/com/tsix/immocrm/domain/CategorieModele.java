package com.tsix.immocrm.domain;

import java.util.Arrays;
import java.util.Optional;

/** Type de fiche source d'un modèle de document (FEATURE-modeles-documents.md §2). */
public enum CategorieModele {

    DOSSIER_VENTE("Dossier de vente"),
    RELANCE_TITRE_FONCIER("Relance titre foncier");

    private final String libelle;

    CategorieModele(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    public static Optional<CategorieModele> depuisCode(String code) {
        return Arrays.stream(values()).filter(c -> c.name().equals(code)).findFirst();
    }
}
