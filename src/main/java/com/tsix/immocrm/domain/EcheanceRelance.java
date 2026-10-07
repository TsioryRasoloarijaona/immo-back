package com.tsix.immocrm.domain;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * Tranche d'échéance de {@code GET /api/dashboard/relances} ; bornes = décision produit,
 * voir {@code instructions/FEATURE-dashboard-relances.md} §3.
 */
public enum EcheanceRelance {

    AUJOURDHUI,

    /**
     * En retard, sur les prospections <b>créées dans le mois civil en cours</b> (pour égaler la liste
     * des prospections) : la borne porte sur {@code ComProspection.DocDate}, pas sur la date de
     * l'alerte. Voir FEATURE-dashboard-relances.md §3 bis.
     */
    RETARD;

    /** {@code null} pour {@link #RETARD} : c'est la date de la prospection qui borne, pas l'alerte. */
    public @Nullable LocalDate debut(LocalDate aujourdhui) {
        return this == AUJOURDHUI ? aujourdhui : null;
    }

    /** Dernier jour d'échéance servi, <b>inclus</b>. */
    public LocalDate fin(LocalDate aujourdhui) {
        return this == AUJOURDHUI ? aujourdhui : aujourdhui.minusDays(1);
    }

    /** Prospections qualifiées, ouvertes et créées dans le mois civil : la population de la liste. */
    public boolean limiteeAuxProspectionsDuMois() {
        return this == RETARD;
    }

    public static LocalDate debutDuMois(LocalDate aujourdhui) {
        return aujourdhui.withDayOfMonth(1);
    }
}
