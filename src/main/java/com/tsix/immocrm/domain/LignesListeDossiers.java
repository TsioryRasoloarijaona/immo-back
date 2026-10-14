package com.tsix.immocrm.domain;

/** Population de la liste des dossiers : OMRAN y mêle les lots libres (statut « Libre »). */
public enum LignesListeDossiers {
    DOSSIERS,
    DOSSIERS_ET_LIBRES,
    LIBRES;

    public boolean avecDossiers() {
        return this != LIBRES;
    }

    public boolean avecLibres() {
        return this != DOSSIERS;
    }
}
