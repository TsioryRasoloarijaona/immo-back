package com.tsix.immocrm.domain;

/** Ordre de restitution des groupes de {@code GET /api/{ressource}/groupes}. */
public enum TriGroupe {

    /** Par libellé croissant, comme le légataire : la liste reste parcourable. Défaut. */
    LIBELLE,

    /** Par effectif décroissant, pour mesurer plutôt que naviguer. */
    NOMBRE;

    public static final TriGroupe DEFAUT = LIBELLE;
}
