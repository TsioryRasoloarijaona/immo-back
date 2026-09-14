package com.tsix.immocrm.domain;

/**
 * Contrat commun aux catalogues de colonnes sélectionnables ({@link ColonneProspection},
 * {@link ColonneProspect}, {@link ColonneDossier}, {@link ColonneEncaissement}).
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
public sealed interface Colonne
        permits ColonneDossier, ColonneEncaissement, ColonneProspect, ColonneProspection {

    /** Nom technique, tel qu'il est passé dans {@code ?colonnes=} et sérialisé en JSON. */
    String nom();

    /** Vrai si la colonne est servie quand {@code ?colonnes=} est absent. */
    boolean parDefaut();

    /**
     * Chemin JPA de la colonne, ou {@code null} si elle n'est ni groupable ni agrégeable.
     *
     * <p>Remonté ici — contrairement au groupe d'affichage et au type, qui restent typés par
     * catalogue — parce que {@link Chemin} est un type <em>partagé</em> : l'exposer sur
     * l'interface ne dégrade rien en {@code String} et ne coûte donc aucune vérification du
     * compilateur. C'est ce qui permet à {@link Colonnes#groupables} d'être écrit une seule fois.
     *
     * <p><strong>Le défaut {@code null} est le comportement voulu</strong>, pas un trou à combler :
     * une colonne n'entre au périmètre du groupement que si on l'y met explicitement, après avoir
     * vérifié son chemin contre l'entité. Les catalogues comptent une soixantaine d'entrées ;
     * les annoter en masse sans mesurer leur cardinalité produirait des groupements que personne
     * ne peut exploiter — voir {@code instructions/FEATURE-groupement-listes.md} §2.
     */
    default Chemin chemin() {
        return null;
    }
}
