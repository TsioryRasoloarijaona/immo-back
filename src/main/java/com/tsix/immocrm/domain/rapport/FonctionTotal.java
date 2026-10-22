package com.tsix.immocrm.domain.rapport;

/** Total d'une colonne, calculé par le serveur ; {@code MOYENNE} = somme / nombre de valeurs non nulles. */
public enum FonctionTotal {
    AUCUNE, SOMME, MOYENNE, MIN, MAX
}
