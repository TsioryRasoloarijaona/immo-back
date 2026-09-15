package com.tsix.immocrm.domain;

import java.time.LocalDate;

/**
 * Tranche d'échéance demandée à {@code GET /api/dashboard/relances}.
 *
 * <p>Les bornes sont ici et non dans le service parce qu'elles portent une décision produit
 * mesurée, pas une commodité technique — voir
 * {@code instructions/FEATURE-dashboard-relances.md} §3.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
 */
public enum EcheanceRelance {

    /** Échéance au jour même. Sur le rythme réel, 1 à 11 relances par jour ouvré. */
    AUJOURDHUI,

    /**
     * Retard <b>borné à 30 jours glissants</b>.
     *
     * <p>Le bornage est la feature, pas une optimisation. En base réelle, 3 710 relances sont en
     * retard, dont <b>2 020 de plus d'un an</b> et seulement 75 de moins de 30 jours : servi sans
     * borne, l'endpoint rendrait un arriéré abandonné au lieu d'une liste de travail. L'arriéré
     * antérieur reste atteignable par les filtres de {@code GET /api/prospections}.
     */
    RETARD;

    /** Nombre de jours de retard au-delà duquel une relance sort de {@link #RETARD}. */
    public static final int FENETRE_RETARD_JOURS = 30;

    /** Premier jour servi, inclus, pour cette tranche évaluée à {@code aujourdhui}. */
    public LocalDate debut(LocalDate aujourdhui) {
        return this == AUJOURDHUI ? aujourdhui : aujourdhui.minusDays(FENETRE_RETARD_JOURS);
    }

    /** Dernier jour servi, <b>inclus</b>, pour cette tranche évaluée à {@code aujourdhui}. */
    public LocalDate fin(LocalDate aujourdhui) {
        return this == AUJOURDHUI ? aujourdhui : aujourdhui.minusDays(1);
    }
}
