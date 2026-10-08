package com.tsix.immocrm.domain;

/**
 * Colonne date de {@code ComFolder} visée par la plage de dates de {@code GET /api/dossiers}.
 * {@link #RESERVATION} lit {@code DocDate} : {@code ReservationDate} n'est presque jamais renseignée.
 */
public enum ChampDateDossier {
    RESERVATION("docDate", "Date de réservation (date de création du dossier)"),
    CONCRETISATION("advanceDate", "Date de concrétisation"),
    COMPROMIS("creditApprovalDate", "Date de compromis / approbation crédit"),
    ENVOI_NOTAIRE("agreementDate", "Date d'envoi au notaire / avocat"),
    CREDIT_DEBLOQUE("creditPossessionDate", "Date de déblocage du crédit"),
    MINUTE("agreementSavingDate", "Date de signature de la minute"),
    REMISE_DES_CLES("deliveryDateReal", "Date de remise des clés effective"),
    FORMALISATION("agreementRegistryDate", "Date d'enregistrement du contrat"),
    ANNULATION("cancelDate", "Date d'annulation");

    private final String champEntite;
    private final String libelle;

    ChampDateDossier(String champEntite, String libelle) {
        this.champEntite = champEntite;
        this.libelle = libelle;
    }

    /** Attribut JPA de {@code ComFolder}, pour {@code root.get(...)}. */
    public String getChampEntite() {
        return champEntite;
    }

    public String getLibelle() {
        return libelle;
    }
}
