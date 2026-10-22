package com.tsix.immocrm.storage;

/**
 * Porteur d'une pièce jointe : segment de clé S3 et {@code AdmAttachedFile.Category} écrite au dépôt. Une énumération,
 * car {@code Category} est du texte en partie libre en données réelles (FEATURE-s3-storage.md §3.5).
 */
public enum PorteurPieceJointe {

    /** Category calculée au dépôt (« Dossier de vente N° {code} du {date} », convention légataire). */
    DOSSIER("dossier", null, "ce dossier"),

    /** Valeur du cas courant ; l'autre dépend de {@code StlSettlement.IsCredit} (voir ReglementPieceJointeService). */
    REGLEMENT("reglement", "Yes.Stl.Credit.Settlement", "ce règlement"),

    ARTICLE("article", "Yes.Stk.Building.Item", "cet article"),

    TRANCHE("tranche", "Yes.Cfg.CfgTranche", "cette tranche"),

    PROJET("projet", "Yes.Cfg.CfgProject", "ce programme"),

    PROSPECTION_TERRAIN("prospection-terrain", "Yes.Lnd.Prospection", "cette prospection");

    private final String segmentCle;
    private final String categoryParDefaut;
    private final String libelle;

    PorteurPieceJointe(String segmentCle, String categoryParDefaut, String libelle) {
        this.segmentCle = segmentCle;
        this.categoryParDefaut = categoryParDefaut;
        this.libelle = libelle;
    }

    public String segmentCle() {
        return segmentCle;
    }

    /** {@code null} si calculée. */
    public String categoryParDefaut() {
        return categoryParDefaut;
    }

    /** Complément des messages 404 (« Pièce jointe introuvable pour ce dossier »). */
    public String libelle() {
        return libelle;
    }
}
