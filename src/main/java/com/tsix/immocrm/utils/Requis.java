package com.tsix.immocrm.utils;

import com.tsix.immocrm.exceptions.InvalidRequestException;

/**
 * Champ obligatoire, validé à la main et non par {@code @Valid} : sans handler de {@code MethodArgumentNotValidException},
 * l'erreur sortirait du format ProblemDetail (FEATURE-reglement-statut-preuve.md §4.4). Message complet fourni par
 * l'appelant : l'accord en genre (« requis » / « requise ») ne se devine pas.
 */
public final class Requis {

    private Requis() {
    }

    /** Renvoie la valeur, ou lève {@link InvalidRequestException} (400) si elle est {@code null}. */
    public static <T> T champ(T valeur, String message) {
        if (valeur == null) {
            throw new InvalidRequestException(message);
        }
        return valeur;
    }
}
