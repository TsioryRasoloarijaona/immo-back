package com.tsix.immocrm.domain;

/** Colonne date de {@code StlSettlement} visée par la période de {@code GET /api/encaissements}. */
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

    /** Attribut JPA de {@code StlSettlement}, pour {@code root.get(...)}. */
    public String getChampEntite() {
        return champEntite;
    }

    public String getLibelle() {
        return libelle;
    }
}
