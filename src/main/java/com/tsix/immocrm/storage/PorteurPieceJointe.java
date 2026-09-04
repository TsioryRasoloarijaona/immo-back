package com.tsix.immocrm.storage;

/**
 * Type d'objet portant une pièce jointe. Sert à deux choses : le segment de clé S3, et la
 * chaîne {@code AdmAttachedFile.Category} écrite au dépôt.
 *
 * <p><b>Pourquoi une énumération plutôt que la colonne {@code Category}</b> : en données
 * réelles, {@code Category} est du texte partiellement libre — à côté des valeurs canoniques
 * {@code Yes.*} on trouve une centaine de libellés du type
 * {@code "Dossier de vente N° ARS 2023/00267 du 18/12/2023"} écrits par l'application Windows,
 * et onze lignes où elle est vide. Elle est inexploitable comme discriminant, et c'est déjà
 * pour cette raison que la relecture ne filtre que sur {@code CategoryId}.
 *
 * <p>Cela absorbe au passage les littéraux qui étaient dispersés dans les trois façades
 * ({@code ReglementPieceJointeService}, {@code PieceJointeService}).
 */
public enum PorteurPieceJointe {

    /**
     * Le {@code Category} d'un dossier reste calculé au dépôt ("Dossier de vente N° {code} du
     * {date}", convention legacy reproduite), d'où l'absence de valeur fixe ici.
     */
    DOSSIER("dossier", null),

    /**
     * Deux valeurs possibles selon {@code StlSettlement.IsCredit} — voir
     * {@code ReglementPieceJointeService}. La valeur portée ici est celle du cas courant
     * (encaissement client, 368 fichiers réels contre 92 en débit).
     */
    REGLEMENT("reglement", "Yes.Stl.Credit.Settlement"),

    ARTICLE("article", "Yes.Stk.Building.Item");

    private final String segmentCle;
    private final String categoryParDefaut;

    PorteurPieceJointe(String segmentCle, String categoryParDefaut) {
        this.segmentCle = segmentCle;
        this.categoryParDefaut = categoryParDefaut;
    }

    /** Segment utilisé dans la clé S3 — voir {@link CleStockage}. */
    public String segmentCle() {
        return segmentCle;
    }

    /** Valeur à écrire dans {@code AdmAttachedFile.Category}, {@code null} si calculée. */
    public String categoryParDefaut() {
        return categoryParDefaut;
    }
}
