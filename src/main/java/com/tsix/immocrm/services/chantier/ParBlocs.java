package com.tsix.immocrm.services.chantier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/** Découpe un {@code IN (:ids)} : SQL Server refuse plus de 2 100 paramètres par requête. */
final class ParBlocs {

    private static final int TAILLE = 1000;

    private ParBlocs() {
    }

    static <I, R> List<R> executer(Collection<I> ids, Function<List<I>, List<R>> requete) {
        List<I> tous = List.copyOf(ids);
        List<R> resultat = new ArrayList<>();
        for (int debut = 0; debut < tous.size(); debut += TAILLE) {
            resultat.addAll(requete.apply(tous.subList(debut, Math.min(debut + TAILLE, tous.size()))));
        }
        return resultat;
    }
}
