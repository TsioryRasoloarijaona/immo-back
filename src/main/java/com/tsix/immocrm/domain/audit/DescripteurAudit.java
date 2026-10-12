package com.tsix.immocrm.domain.audit;

import java.util.function.Function;

/**
 * Ce que l'audit écrit pour une classe d'entité : son type OMRAN (qui peut dépendre de la ligne), son libellé
 * ({@code ObjectLabel} = {@code Description} de la ligne d'audit) et, pour une ligne enfant, son parent.
 */
public record DescripteurAudit<T>(Class<T> classe,
                                  Function<T, TypeObjetAudit> type,
                                  Function<T, String> libelle,
                                  Function<T, ParentAudit> parent) {

    public TypeObjetAudit typeDe(Object entite) {
        return type.apply(classe.cast(entite));
    }

    public String libelleDe(Object entite) {
        String valeur = libelle.apply(classe.cast(entite));
        return valeur == null ? "" : valeur;
    }

    /** {@code null} pour un objet racine. */
    public ParentAudit parentDe(Object entite) {
        return parent.apply(classe.cast(entite));
    }
}
