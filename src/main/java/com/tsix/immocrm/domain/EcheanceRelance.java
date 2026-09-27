package com.tsix.immocrm.domain;

import org.jspecify.annotations.Nullable;

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
     * En retard, <b>sur les prospections créées dans le mois civil en cours</b>.
     *
     * <p>Décision produit du 28/09/2026, qui remplace la fenêtre glissante de 30 jours du
     * 15/09 : le tableau de bord doit montrer les mêmes relances que la liste des prospections,
     * et cette liste travaille sur les prospections <b>du mois</b>. Avec l'ancienne fenêtre le
     * tableau de bord en comptait 81 pour une commerciale quand sa liste en montrait 45 — les 33
     * autres étaient portées par des prospections du mois précédent, 5 par des pré-prospections.
     *
     * <p>La borne porte donc sur la <b>date de création de la prospection</b>
     * ({@code ComProspection.DocDate}), plus sur la date de l'alerte : toute alerte échue d'une
     * prospection du mois compte, quelle que soit son ancienneté. Le mur des retards anciens
     * (3 161 lignes) reste hors du tableau de bord, puisqu'il est porté par des prospections
     * anciennes ; il reste atteignable par les filtres de {@code GET /api/prospections}.
     */
    RETARD;

    /**
     * Premier jour d'échéance servi, inclus. {@code null} pour {@link #RETARD} : aucune borne
     * basse sur la date de l'alerte, c'est la date de la prospection qui borne.
     */
    public @Nullable LocalDate debut(LocalDate aujourdhui) {
        return this == AUJOURDHUI ? aujourdhui : null;
    }

    /** Dernier jour d'échéance servi, <b>inclus</b>, pour cette tranche évaluée à {@code aujourdhui}. */
    public LocalDate fin(LocalDate aujourdhui) {
        return this == AUJOURDHUI ? aujourdhui : aujourdhui.minusDays(1);
    }

    /**
     * Vrai si la tranche ne retient que les prospections <b>qualifiées, ouvertes et créées dans
     * le mois civil</b> de {@code aujourdhui} — la population de la liste des prospections.
     */
    public boolean limiteeAuxProspectionsDuMois() {
        return this == RETARD;
    }

    /** Premier jour du mois civil de {@code aujourdhui}, inclus. */
    public static LocalDate debutDuMois(LocalDate aujourdhui) {
        return aujourdhui.withDayOfMonth(1);
    }
}
