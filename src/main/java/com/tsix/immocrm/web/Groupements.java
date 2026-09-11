package com.tsix.immocrm.web;

import com.tsix.immocrm.domain.Colonne;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.util.function.Function;

/**
 * Résolution des paramètres de groupement communs aux contrôleurs de listes.
 *
 * <p>Écrite une fois pour {@code DossierController}, {@code ProspectionController} et
 * {@code PreProspectionController} : les trois reçoivent les mêmes paramètres et doivent les
 * refuser de la même façon. Voir {@code instructions/FEATURE-groupement-listes.md} §4.
 */
public final class Groupements {

    private Groupements() {
    }

    /**
     * Résout {@code ?groupeColonne=} pour le dépliage d'un groupe.
     *
     * <p><strong>{@code groupeValeur} sans {@code groupeColonne} est un 400, pas un paramètre
     * ignoré.</strong> Ignorer silencieusement renverrait la liste entière alors que le client
     * croit avoir ouvert un groupe — une page de résultats plausible mais fausse, exactement le
     * genre d'erreur qui ne se voit pas.
     *
     * <p>L'inverse est licite : {@code groupeColonne} seul désigne le groupe des lignes sans
     * valeur, qui doit pouvoir s'ouvrir comme les autres.
     *
     * @return la colonne groupée, ou {@code null} si aucun dépliage n'est demandé
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

    /**
     * Granularité effective d'un groupement.
     *
     * <p>{@code null} dès que la colonne n'est pas temporelle — y compris si le client a passé une
     * granularité, qui est alors sans objet. La renvoyer quand même ferait appliquer
     * {@code YEAR()} à un code de statut.
     */
    public static Granularite granularite(Colonne colonne, Granularite demandee) {
        if (colonne == null || colonne.chemin() == null || !colonne.chemin().temporel()) {
            return null;
        }
        return demandee != null ? demandee : Granularite.DEFAUT;
    }
}
