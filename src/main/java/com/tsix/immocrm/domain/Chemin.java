package com.tsix.immocrm.domain;

import java.util.List;

/**
 * Chemin JPA (navigation pointée) d'une colonne de catalogue, pour le groupement et les agrégats.
 * Clé et libellé sont dissociés : un tiers se groupe sur son id, son nom n'existe pas en base.
 * Groupable et agrégeable ne s'excluent pas : voir FEATURE-groupement-toutes-colonnes.md §1.
 */
public record Chemin(
        String cheminCle,
        List<String> cheminsLibelle,
        TypeLibelle typeLibelle,
        boolean groupable,
        boolean agregeable,
        boolean temporel,
        String cheminSoustrait,
        Exclusion exclusion) {

    /** Lignes laissées hors des SUM/AVG de la colonne (pas du comptage) : {@code chemin = valeur}. */
    public record Exclusion(String chemin, String valeur) {
    }

    public Chemin(String cheminCle, List<String> cheminsLibelle, TypeLibelle typeLibelle, boolean groupable,
                  boolean agregeable, boolean temporel, String cheminSoustrait) {
        this(cheminCle, cheminsLibelle, typeLibelle, groupable, agregeable, temporel, cheminSoustrait, null);
    }

    /** Même chemin, dont les sommes ignorent les lignes où {@code chemin} vaut {@code valeur}. */
    public Chemin sauf(String chemin, String valeur) {
        return new Chemin(cheminCle, cheminsLibelle, typeLibelle, groupable, agregeable, temporel, cheminSoustrait,
                new Exclusion(chemin, valeur));
    }

    public enum TypeLibelle {
        DIRECT,
        /** Priorité {@code socialReason > description > firstName + lastName}, cf. TierNomFormatter. */
        TIER,
        /** La clé est le premier des {@link #cheminsLibelle()} qui porte une valeur non vide. */
        REPLI
    }

    /**
     * Valeur = première colonne non vide, dans l'ordre de priorité. La clé SQL met un {@code NULLIF}
     * sur chaîne vide à chaque terme, comme {@code hasText} en Java : sinon un champ vide mais non nul
     * forme son propre groupe alors que la ligne affiche la colonne suivante.
     */
    public static Chemin repli(String... chemins) {
        return new Chemin(chemins[0], List.of(chemins), TypeLibelle.REPLI, true, false, false, null);
    }

    public static Chemin groupable(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, true, false, false, null);
    }

    /**
     * Date groupable, par {@link Granularite} seulement ; MIN/MAX sans SUM. Marquée ici car
     * {@code TypeColonne} est un enum distinct par catalogue, illisible depuis le code générique.
     */
    public static Chemin date(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, true, false, true, null);
    }

    /**
     * Association {@code CfgTier} : groupe sur l'id, sélectionne les trois colonnes de nom pour que
     * le libellé suive exactement la priorité de {@code TierNomFormatter} (sinon groupe ≠ ligne).
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

    public static Chemin agregeable(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, false, true, false, null);
    }

    public static Chemin groupableEtAgregeable(String chemin) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, true, true, false, null);
    }

    /**
     * Différence de deux chemins joints, le second pré-agrégé en {@code @Subselect} : une
     * {@code @Formula} à sous-requête casse {@code SUM()} (SQL Server, erreur 130 — voir
     * FEATURE-colonnes-groupement-encaissements.md §7.2). Ni MIN ni MAX : termes agrégés séparément.
     */
    public static Chemin difference(String chemin, String cheminSoustrait) {
        return new Chemin(chemin, List.of(chemin), TypeLibelle.DIRECT, false, true, false,
                cheminSoustrait);
    }
}
