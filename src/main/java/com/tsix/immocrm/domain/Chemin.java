package com.tsix.immocrm.domain;

import java.util.List;

/**
 * Chemin d'accès JPA d'une colonne de catalogue, pour le groupement et les agrégats.
 *
 * <h2>Pourquoi ce type existe</h2>
 *
 * <p>Les catalogues ({@link ColonneDossier}, {@link ColonneProspection}, {@link ColonneProspect})
 * servaient jusqu'ici à <strong>filtrer la sérialisation</strong> : ils disent quel champ du DTO
 * renvoyer, pas où le lire. Le mappeur {@code toListItem} est positionnel et n'expose aucune
 * correspondance nom → chemin.
 *
 * <p>Grouper exige exactement cette correspondance : {@code ?groupBy=statut} doit se traduire en
 * {@code GROUP BY strStatus.code}. C'est la seule pièce qui manquait — voir
 * {@code instructions/FEATURE-groupement-listes.md} §3.
 *
 * <h2>Pourquoi la clé et le libellé sont dissociés</h2>
 *
 * <p>Grouper par client ne peut pas grouper sur le nom affiché : {@link
 * com.tsix.immocrm.utils.TierNomFormatter} le dérive en Java de trois colonnes selon une priorité
 * ({@code socialReason} &gt; {@code description} &gt; {@code firstName + lastName}), il n'existe
 * nulle part en base. On groupe donc sur {@code cfgCustomer.id} — stable, indexé, et qui ne
 * fusionne pas deux homonymes — et on sélectionne les trois colonnes de nom <em>en plus</em> pour
 * reconstituer le libellé. D'où {@link #cheminsLibelle()}, distinct de {@link #cheminCle()}.
 *
 * <p>Pour les colonnes ordinaires ({@code strStatus.code}), clé et libellé sont le même chemin et
 * {@link TypeLibelle#DIRECT} s'applique.
 *
 * <h2>Groupable et agrégeable s'excluent en pratique</h2>
 *
 * <p>Mesuré sur les 358 dossiers réels : {@code Price} porte 190 valeurs distinctes, {@code DocDate}
 * 214. Grouper un montant ou une date exacte ne produit pas de l'information, ça produit une liste
 * plate avec des en-têtes. Les montants s'additionnent, les dates se groupent par
 * {@link Granularite}. La règle complète est en §2 du FEATURE.
 *
 * <p>Sans JPA, comme le reste de {@code domain/} : ce sont des chaînes de navigation, résolues par
 * la couche service.
 *
 * @param cheminCle      chemin de la clé de groupe, tel qu'attendu par {@code Path#get} en
 *                       navigation pointée ({@code "stkItem.stkHierarchy.code"})
 * @param cheminsLibelle chemins à sélectionner en plus pour reconstituer le libellé affiché ;
 *                       identique à {@code cheminCle} pour {@link TypeLibelle#DIRECT}
 * @param typeLibelle    comment reconstituer le libellé à partir des valeurs sélectionnées
 * @param groupable      exposé au paramètre {@code ?groupBy=}
 * @param agregeable     exposé au paramètre {@code ?agregats=} (somme)
 * @param temporel       colonne date : la clé de groupe passe par une {@link Granularite}
 */
public record Chemin(
        String cheminCle,
        List<String> cheminsLibelle,
        TypeLibelle typeLibelle,
        boolean groupable,
        boolean agregeable,
        boolean temporel) {

    /** Comment le libellé d'un groupe se reconstitue à partir des colonnes sélectionnées. */
    public enum TypeLibelle {
        /** Le libellé est la valeur de la clé elle-même. */
        DIRECT,
        /** Priorité {@code socialReason > description > firstName + lastName}, cf. TierNomFormatter. */
        TIER
    }

    /** Colonne groupable dont la valeur affichée est la clé — le cas courant. */
    public static Chemin groupable(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, true, false, false);
    }

    /**
     * Colonne date groupable, dont la clé passe par une {@link Granularite}.
     *
     * <p>Marquée ici plutôt que déduite du {@code TypeColonne} du catalogue : celui-ci est un enum
     * <em>imbriqué dans chaque catalogue</em> ({@code ColonneDossier.TypeColonne} et
     * {@code ColonneProspection.TypeColonne} sont deux types distincts), donc illisible depuis du
     * code générique. Le porter sur {@link Chemin}, qui est partagé, évite d'avoir à dégrader ces
     * enums en {@code String} — le compromis que le javadoc de {@link Colonne} refuse.
     */
    public static Chemin date(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, true, false, true);
    }

    /**
     * Colonne groupable pointant une association {@code CfgTier} (client, commercial, notaire).
     *
     * <p>Groupe sur l'identifiant et sélectionne les trois colonnes de nom pour que le libellé
     * suive exactement la priorité de {@code TierNomFormatter}. Aligner les deux n'est pas
     * cosmétique : un groupe libellé autrement que la ligne qu'il contient est un bug d'écran.
     *
     * @param association nom du champ d'association sur l'entité racine ({@code "cfgCustomer"})
     */
    public static Chemin tier(String association) {
        return new Chemin(
                association + ".id",
                List.of(association + ".socialReason",
                        association + ".description",
                        association + ".firstName",
                        association + ".lastName"),
                TypeLibelle.TIER,
                true,
                false,
                false);
    }

    /** Colonne sommable, destinée aux bandes « Total : » — non groupable, cf. la mesure ci-dessus. */
    public static Chemin agregeable(String chemin) {
        return new Chemin(chemin, List.of(), TypeLibelle.DIRECT, false, true, false);
    }
}
