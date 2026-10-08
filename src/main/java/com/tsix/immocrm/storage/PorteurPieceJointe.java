package com.tsix.immocrm.storage;

/**
 * Porteur d'une pièce jointe : segment de clé S3 et {@code AdmAttachedFile.Category} écrite au dépôt. Une énumération,
 * car {@code Category} est du texte en partie libre en données réelles (FEATURE-s3-storage.md §3.5).
 */
public enum PorteurPieceJointe {

    /** Category calculée au dépôt (« Dossier de vente N° {code} du {date} », convention légataire). */
    DOSSIER("dossier", null),

    /** Valeur du cas courant ; l'autre dépend de {@code StlSettlement.IsCredit} (voir ReglementPieceJointeService). */
    REGLEMENT("reglement", "Yes.Stl.Credit.Settlement"),

    ARTICLE("article", "Yes.Stk.Building.Item");

    private final String segmentCle;
    private final String categoryParDefaut;

    PorteurPieceJointe(String segmentCle, String categoryParDefaut) {
        this.segmentCle = segmentCle;
        this.categoryParDefaut = categoryParDefaut;
    }

    public String segmentCle() {
        return segmentCle;
    }

    /** {@code null} si calculée. */
    public String categoryParDefaut() {
        return categoryParDefaut;
    }
}
