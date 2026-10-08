package com.tsix.immocrm.web;

import com.tsix.immocrm.domain.Colonne;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.util.function.Function;

/** Paramètres de groupement communs aux listes, refusés pareil partout (FEATURE-groupement-listes.md §4). */
public final class Groupements {

    private Groupements() {
    }

    /**
     * {@code groupeValeur} sans {@code groupeColonne} = 400 : l'ignorer renverrait la liste entière, plausible mais
     * fausse. {@code groupeColonne} seul est licite (groupe des lignes sans valeur). Nul si pas de dépliage.
     */
    public static <C extends Colonne> C colonneDeDepliage(String groupeColonne, String groupeValeur,
                                                          Function<String, C> resolveur) {
        if (groupeColonne == null || groupeColonne.isBlank()) {
            if (groupeValeur != null && !groupeValeur.isBlank()) {
                throw new InvalidRequestException(
                        "groupeValeur ne peut pas être fourni sans groupeColonne : la valeur d'un "
                        + "groupe n'a de sens que rapportée à la colonne qui a servi au groupement.");
            }
            return null;
        }
        return resolveur.apply(groupeColonne);
    }

    /** Nulle si la colonne n'est pas temporelle, même si le client en a passé une (sinon {@code YEAR()} sur un code). */
    public static Granularite granularite(Colonne colonne, Granularite demandee) {
        if (colonne == null || colonne.chemin() == null || !colonne.chemin().temporel()) {
            return null;
        }
        return demandee != null ? demandee : Granularite.DEFAUT;
    }
}
