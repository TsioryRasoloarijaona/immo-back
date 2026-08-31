package com.tsix.immocrm.domain;

/**
 * Which {@code StlSettlement} date column a "période" filter on {@code GET /api/encaissements}
 * applies to — mirrors the legacy "Encaissements Vente" screen's period dropdown ("Date pièce"
 * by default, with "Date échéance"/"Date règlement" as alternatives). Same pattern as
 * {@link ChampDateDossier} for Dossier listings.
 */
public enum ChampDateEncaissement {
    DATE_PIECE("docDate", "Date pièce (date de saisie du règlement)"),
    DATE_ECHEANCE("echeanceDate", "Date d'échéance"),
    DATE_REGLEMENT("settlementDate", "Date de règlement effectif");

    private final String champEntite;
    private final String libelle;

    ChampDateEncaissement(String champEntite, String libelle) {
        this.champEntite = champEntite;
        this.libelle = libelle;
    }

    /** JPA entity attribute name on {@code StlSettlement} — usable directly in a Criteria {@code root.get(...)}. */
    public String getChampEntite() {
        return champEntite;
    }

    public String getLibelle() {
        return libelle;
    }
}
