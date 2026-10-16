package com.tsix.immocrm.domain;

/** Date sur laquelle porte la période des prospections ({@code ProspectionList.GetDatesFilter} d'OMRAN). */
public enum PeriodeSur {

    /** {@code ComProspection.DocDate}. */
    DATE("docDate"),

    /** {@code ComProspection.ClosingDate}. */
    ARCHIVAGE("closingDate");

    private final String attribut;

    PeriodeSur(String attribut) {
        this.attribut = attribut;
    }

    /** Attribut JPA de {@code ComProspection} portant la date. */
    public String attribut() {
        return attribut;
    }
}
