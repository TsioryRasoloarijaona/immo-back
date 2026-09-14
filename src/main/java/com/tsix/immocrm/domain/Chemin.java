package com.tsix.immocrm.domain;

import java.util.List;

/**
 * Chemin d'accès JPA d'une colonne de catalogue, pour le groupement et les agrégats.
 *
 * <h2>Pourquoi ce type existe</h2>
 *
 * <p>Les catalogues ({@link ColonneDossier}, {@link ColonneProspection}, {@link ColonneProspect},
 * {@link ColonneEncaissement}) servaient jusqu'ici à <strong>filtrer la sérialisation</strong> :
 * ils disent quel champ du DTO renvoyer, pas où le lire. Le mappeur {@code toListItem} est
 * positionnel et n'expose aucune correspondance nom → chemin.
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
 * <h2>Groupable et agrégeable ne s'excluent pas</h2>
 *
 * <p>Ils l'ont fait, et c'était une erreur de périmètre. Mesuré sur les 358 dossiers réels,
 * {@code Price} porte 190 valeurs distinctes et {@code DocDate} 214 : grouper dessus rend presque
 * autant de groupes que de lignes. J'en avais conclu qu'il ne fallait pas l'offrir. <strong>C'est
 * au client de juger de l'utilité d'un groupement, pas au catalogue</strong> — un portefeuille
 * filtré sur une tranche peut parfaitement n'avoir que six prix distincts, et le groupement y est
 * alors la bonne lecture.
 *
 * <p>Une colonne monétaire est donc désormais {@link #groupableEtAgregeable} : clé de groupe
 * <em>et</em> total. Ce qui reste vrai de la mesure, c'est que grouper une date sur sa valeur
 * exacte n'a pas de sens — d'où {@link Granularite}, qui n'est pas une option mais le seul mode
 * de groupement d'une date.
 *
 * <p>Quelles <em>fonctions</em> d'agrégat un chemin autorise se déduit d'ici et de nulle part
 * ailleurs : voir {@link FonctionAgregat#pour(Chemin)}.
 *
 * <p>Sans JPA, comme le reste de {@code domain/} : ce sont des chaînes de navigation, résolues par
 * la couche service.
 *
 * @param cheminCle       chemin de la clé de groupe, tel qu'attendu par {@code Path#get} en
 *                        navigation pointée ({@code "stkItem.stkHierarchy.code"})
 * @param cheminsLibelle  chemins à sélectionner en plus pour reconstituer le libellé affiché ;
 *                        identique à {@code cheminCle} pour {@link TypeLibelle#DIRECT}
 * @param typeLibelle     comment reconstituer le libellé à partir des valeurs sélectionnées
 * @param groupable       exposé au paramètre {@code ?groupBy=}
 * @param agregeable      exposé au paramètre {@code ?agregats=}
 * @param temporel        colonne date : la clé de groupe passe par une {@link Granularite}
 * @param cheminSoustrait chemin à retrancher de {@code cheminCle} pour former la valeur agrégée,
 *                        ou {@code null} pour le cas courant — voir {@link #difference}
 */
public record Chemin(
        String cheminCle,
        List<String> cheminsLibelle,
        TypeLibelle typeLibelle,
        boolean groupable,
        boolean agregeable,
        boolean temporel,
        String cheminSoustrait) {

    /** Comment le libellé d'un groupe se reconstitue à partir des colonnes sélectionnées. */
    public enum TypeLibelle {
        /** Le libellé est la valeur de la clé elle-même. */
        DIRECT,
        /** Priorité {@code socialReason > description > firstName + lastName}, cf. TierNomFormatter. */
        TIER,
        /** La clé est le premier des {@link #cheminsLibelle()} qui porte une valeur non vide. */
        REPLI
    }

    /**
     * Colonne dont la valeur affichée est un <strong>repli</strong> sur plusieurs colonnes.
     *
     * <p>Le cas de {@code telephone} et {@code email} de la prospection : la liste sert la fiche
     * d'abord, le champ saisi au premier contact ensuite. Mesuré sur les 8 613 prospections
     * ouvertes — 5 034 renseignées par la fiche, 7 596 par le champ temporaire, <strong>8 591 par
     * l'une ou l'autre</strong> : aucune source ne suffit seule, le repli n'est pas une commodité.
     *
     * <p>La clé de groupe rejoue donc la même cascade en SQL. Elle doit la rejouer
     * <em>exactement</em> : le Java teste {@code StringUtils.hasText}, qui rejette aussi les
     * chaînes blanches, là où un {@code COALESCE} nu ne rejette que {@code NULL}. D'où le
     * {@code NULLIF} sur chaîne vide appliqué à chaque terme — sans lui, une fiche au mobile vide
     * mais non nul formerait son propre groupe alors que la ligne affiche le numéro temporaire.
     *
     * @param chemins les colonnes dans l'ordre de priorité
     */
    public static Chemin repli(String... chemins) {
        return new Chemin(chemins[0], List.of(chemins), TypeLibelle.REPLI, true, false, false, null);
    }

    /** Colonne groupable dont la valeur affichée est la clé — le cas courant. */
    public static Chemin groupable(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, true, false, false, null);
    }

    /**
     * Colonne date groupable, dont la clé passe par une {@link Granularite}.
     *
     * <p>Marquée ici plutôt que déduite du {@code TypeColonne} du catalogue : celui-ci est un enum
     * <em>imbriqué dans chaque catalogue</em> ({@code ColonneDossier.TypeColonne} et
     * {@code ColonneProspection.TypeColonne} sont deux types distincts), donc illisible depuis du
     * code générique. Le porter sur {@link Chemin}, qui est partagé, évite d'avoir à dégrader ces
     * enums en {@code String} — le compromis que le javadoc de {@link Colonne} refuse.
     *
     * <p>C'est aussi ce qui permet à une date d'offrir {@code MIN}/{@code MAX} sans offrir
     * {@code SUM}, cf. {@link FonctionAgregat#pour(Chemin)}.
     */
    public static Chemin date(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, true, false, true, null);
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
                false,
                null);
    }

    /** Colonne sommable seulement — réservée aux cas où grouper n'a pas de sens exprimable. */
    public static Chemin agregeable(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, false, true, false, null);
    }

    /**
     * Colonne monétaire ou numérique : clé de groupe <strong>et</strong> total.
     *
     * <p>Le cas normal pour un {@code MONTANT}/{@code NOMBRE}. Voir l'entête sur la non-exclusivité
     * des deux rôles.
     */
    public static Chemin groupableEtAgregeable(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, true, true, false, null);
    }

    /**
     * Colonne dont la valeur est la <strong>différence de deux colonnes</strong>, dont l'une vit
     * sur une association pré-agrégée.
     *
     * <h2>Pourquoi ce détour plutôt qu'une {@code @Formula}</h2>
     *
     * <p>C'est le cas de {@code montantBrutRestant} : montant du règlement moins la part déjà
     * affectée à une échéance, cette part étant elle-même une somme de lignes. La voie évidente
     * serait une {@code @Formula} portant une sous-requête corrélée, comme
     * {@code ComFolder.delaiConcretisation}. <strong>Elle ne marche pas</strong> : SQL Server
     * refuse {@code SUM()} sur une expression contenant une sous-requête — erreur 130,
     * « impossible d'effectuer une fonction d'agrégation sur une expression contenant une
     * agrégation ou une sous-requête ». Mesuré, pas supposé.
     *
     * <p>La part affectée est donc pré-agrégée dans une entité {@code @Subselect} joignable, et la
     * différence s'exprime sur <em>deux chemins joints</em>. Mesuré à 7 ms sur les 643 règlements.
     *
     * <h2>Ni groupable, ni minimisable</h2>
     *
     * <p>Non groupable : même statut que {@code delaiConcretisation}, et sur données réelles cette
     * colonne vaut zéro partout — le groupement rendrait un groupe unique.
     *
     * <p>{@link FonctionAgregat#pour(Chemin)} n'offre ici <strong>ni MIN ni MAX</strong> : le
     * moteur agrège les deux termes séparément puis soustrait, ce qui est exact pour un total et
     * une moyenne (les deux sont linéaires) et faux pour un extremum.
     *
     * @param chemin          le terme positif ({@code "amount"})
     * @param cheminSoustrait le terme retranché ({@code "affectation.montantAffecte"})
     */
    public static Chemin difference(String chemin, String cheminSoustrait) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, false, true, false,
                cheminSoustrait);
    }
}
