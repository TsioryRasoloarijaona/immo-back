package com.tsix.immocrm.domain;

import java.util.List;

/**
 * Fonction d'agrégation (menu « Σ » du légataire). Les fonctions permises se déduisent du
 * {@link Chemin} ({@code agregeable}, {@code temporel}) : {@code TypeColonne} varie par catalogue.
 */
public enum FonctionAgregat {

    /** Total, et le défaut. */
    SUM,
    /** Calculée en Java (total ÷ effectif non nul), jamais {@code cb.avg()} qui rend un {@code double}. */
    AVG,
    MIN,
    MAX,
    /** Nombre de lignes du groupe dont la colonne est renseignée. */
    COUNT;

    /** Nom de colonne nu ({@code ?agregats=prix}) : somme sous la même clé qu'avant le menu Σ. */
    public static final FonctionAgregat DEFAUT = SUM;

    /** Séparateur de {@code colonne:FONCTION} dans {@code ?agregats=}. */
    public static final char SEPARATEUR = ':';

    /**
     * Dans l'ordre où le sélecteur les propose. Sans chemin : aucune. Différence : ni MIN ni MAX,
     * {@code MIN(a - b)} ne se déduit pas de {@code MIN(a)} et {@code MIN(b)}.
     */
    public static List<FonctionAgregat> pour(Chemin chemin) {
        if (chemin == null) {
            return List.of();
        }
        if (chemin.cheminSoustrait() != null) {
            return List.of(SUM, AVG, COUNT);
        }
        if (chemin.agregeable()) {
            return List.of(SUM, AVG, MIN, MAX, COUNT);
        }
        if (chemin.temporel()) {
            return List.of(MIN, MAX, COUNT);
        }
        return List.of(COUNT);
    }
}
