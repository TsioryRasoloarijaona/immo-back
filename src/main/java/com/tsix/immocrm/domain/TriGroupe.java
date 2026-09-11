package com.tsix.immocrm.domain;

/**
 * Ordre de restitution des groupes de {@code GET /api/{ressource}/groupes}.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
 */
public enum TriGroupe {

    /**
     * Par libellé croissant — l'ordre du logiciel de bureau, dont la bande de groupement porte un
     * chevron de tri sur le nom de la colonne groupée. C'est le défaut : il rend la liste
     * parcourable (on cherche « Adnane » là où on cherche « Adnane »), ce que l'ordre par effectif
     * ne permet pas.
     */
    LIBELLE,

    /**
     * Par effectif décroissant. Utile quand le groupement sert à mesurer plutôt qu'à naviguer —
     * « quels commerciaux portent le plus de dossiers », « quelle origine amène le plus de
     * prospections ».
     */
    NOMBRE;

    public static final TriGroupe DEFAUT = LIBELLE;
}
