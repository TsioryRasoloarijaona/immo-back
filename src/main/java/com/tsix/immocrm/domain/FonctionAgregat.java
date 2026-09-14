package com.tsix.immocrm.domain;

import java.util.List;

/**
 * Fonction d'agrégation applicable à une colonne de catalogue — le menu « Σ » que le logiciel de
 * bureau ouvre sur chaque en-tête de colonne.
 *
 * <h2>Pourquoi les fonctions se déduisent du {@link Chemin}</h2>
 *
 * <p>Sommer une date n'a pas de sens, et la moyenne d'un code de statut non plus. Il faut donc
 * savoir de quel genre est la colonne — mais le {@code TypeColonne} est un enum <em>imbriqué dans
 * chaque catalogue</em> ({@code ColonneDossier.TypeColonne} et {@code ColonneEncaissement.TypeColonne}
 * sont deux types distincts), illisible depuis du code générique. Le javadoc de {@link Colonne}
 * explique pourquoi on refuse de les dégrader en {@code String}.
 *
 * <p>{@link Chemin} porte déjà exactement ce qu'il faut : {@code agregeable} dit « numérique »,
 * {@code temporel} dit « date ». Aucun nouveau champ à déclarer dans les catalogues.
 */
public enum FonctionAgregat {

    /** Total. La seule fonction offerte avant l'ouverture du menu Σ, et donc le défaut. */
    SUM,
    /**
     * Moyenne. <strong>Calculée en Java</strong> à partir du total et de l'effectif non nul, jamais
     * par {@code cb.avg()} qui rend un {@code double} — voir {@code GroupementService}.
     */
    AVG,
    MIN,
    MAX,
    /** Nombre de lignes du groupe dont la colonne est renseignée. */
    COUNT;

    /**
     * Fonction appliquée quand le client écrit un nom de colonne nu ({@code ?agregats=prix}).
     *
     * <p><strong>C'est ce qui rend l'ouverture du menu Σ non cassante</strong> : un appelant écrit
     * depuis toujours des noms nus, il continue d'obtenir des sommes sous les mêmes clés.
     */
    public static final FonctionAgregat DEFAUT = SUM;

    /** Séparateur de {@code colonne:FONCTION} dans {@code ?agregats=}. */
    public static final char SEPARATEUR = ':';

    /**
     * Fonctions qu'un chemin autorise, dans l'ordre où le sélecteur doit les proposer.
     *
     * <p>Une colonne sans chemin n'en autorise aucune — elle n'existe pas en base, il n'y a rien à
     * agréger. C'est le cas de {@code dossierId} côté encaissements (association to-many) comme de
     * {@code montantNet} avant que ce lot ne le mappe.
     *
     * <p>Un chemin en différence ({@link Chemin#difference}) n'offre <strong>ni MIN ni MAX</strong> :
     * {@code MIN(a - b)} ne se déduit pas de {@code MIN(a)} et {@code MIN(b)}, alors que le total et
     * la moyenne, eux, sont linéaires et restent exacts. Offrir un minimum qu'on ne sait pas
     * calculer juste serait pire que ne pas l'offrir.
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
