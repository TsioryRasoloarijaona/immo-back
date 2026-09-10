package com.tsix.immocrm.domain;

/**
 * Contrat commun aux catalogues de colonnes sélectionnables ({@link ColonneProspection},
 * {@link ColonneProspect}, {@link ColonneDossier}).
 *
 * <p>Existe pour que la résolution d'un {@code ?colonnes=} soit écrite <strong>une seule
 * fois</strong>, dans {@link Colonnes}, plutôt que recopiée par catalogue. Trois copies d'une
 * même vingtaine de lignes finiraient par diverger — c'est exactement ce qui était arrivé aux
 * trois mappeurs de {@code ProspectionListItemDto}.
 *
 * <p><strong>Volontairement réduit au strict nécessaire.</strong> Le groupe d'affichage et le type
 * restent typés par catalogue (chacun a son propre {@code enum Groupe}) : les remonter ici
 * imposerait de les dégrader en {@code String} pour tout le monde, ce qui ferait perdre la
 * vérification du compilateur là où elle sert — au moment de déclarer une colonne.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
 */
public interface Colonne {

    /** Nom technique, tel qu'il est passé dans {@code ?colonnes=} et sérialisé en JSON. */
    String nom();

    /** Vrai si la colonne est servie quand {@code ?colonnes=} est absent. */
    boolean parDefaut();
}
