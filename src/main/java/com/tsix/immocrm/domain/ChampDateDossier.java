package com.tsix.immocrm.domain;

/**
 * Which {@code ComFolder} date column a "plage de dates" filter on {@code GET /api/dossiers}
 * applies to — lets the front pick a milestone the way the legacy Omran panel does ("Date
 * réservation", "Date concrétisation", ...) instead of always filtering on the creation date.
 * Reuses the same confirmed columns as {@link EtapeMilestone} (only the ones with a real, mapped
 * column — see that class for how each was confirmed); {@link #RESERVATION} is the one exception,
 * mapped to {@code DocDate} rather than the real {@code ReservationDate} column, which is almost
 * never populated in practice (2/374 real dossiers) — same choice {@link EtapeMilestone} already
 * made for the RESERVATION status. See {@code instructions/filtres-avances.md} point 3.
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

    /** JPA entity attribute name on {@code ComFolder} — usable directly in a Criteria {@code root.get(...)}. */
    public String getChampEntite() {
        return champEntite;
    }

    public String getLibelle() {
        return libelle;
    }
}
