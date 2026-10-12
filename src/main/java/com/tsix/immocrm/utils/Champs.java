package com.tsix.immocrm.utils;

import java.util.function.Function;

/**
 * Lecture d'un champ à travers une association optionnelle, sans évaluer deux fois le getter intermédiaire (deux
 * passages par le proxy LAZY). Pas de variante {@code Optional} : on l'emballerait pour le déballer aussitôt.
 */
public final class Champs {

    private Champs() {
    }

    /** {@code de(tranche, CfgTranche::getCode)} : le champ, ou {@code null} si l'association l'est. */
    public static <T, R> R de(T source, Function<T, R> lecteur) {
        return source == null ? null : lecteur.apply(source);
    }

    /** {@code de(dossier, ComFolder::getStrStatus, StrStatus::getCode)} : {@code null} dès qu'un maillon manque. */
    public static <T, U, R> R de(T source, Function<T, U> premier, Function<U, R> second) {
        U intermediaire = de(source, premier);
        return intermediaire == null ? null : second.apply(intermediaire);
    }

    /** Texte légataire écrit {@code ""} (OMRAN n'écrit jamais NULL) relu comme absent : {@code null}, sinon rogné. */
    public static String videEnNull(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }
}
