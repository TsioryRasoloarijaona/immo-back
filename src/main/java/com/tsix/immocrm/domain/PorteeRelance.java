package com.tsix.immocrm.domain;

/**
 * À qui s'adresse {@code GET /api/dashboard/relances}, <b>par-dessus</b> le périmètre commercial.
 * Les relances non affectées sont servies dans les deux cas — FEATURE-dashboard-relances.md §4.
 */
public enum PorteeRelance {

    /** Les relances dont le commercial résolu est l'appelant, plus les non affectées. */
    MOI,

    /** Tout le périmètre visible de l'appelant, sans distinction de titulaire. */
    EQUIPE
}
