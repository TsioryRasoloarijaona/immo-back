package com.tsix.immocrm.services.equipe;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Seule règle « lead endormi » de « Mon équipe » : la colonne « Sans action > 7 j » et la fenêtre « Leads de … »
 * passent par {@link #seuil} (FEATURE-leads-a-relancer.md §Définitions).
 */
public final class ActiviteLead {

    public static final int JOURS_SANS_ACTION = 7;

    private ActiviteLead() {
    }

    /** Une dernière action (à défaut la date du lead) strictement antérieure à ce seuil = endormi. */
    public static LocalDateTime seuil(LocalDateTime maintenant) {
        return maintenant.minusDays(JOURS_SANS_ACTION);
    }

    public static LocalDateTime reference(@Nullable LocalDateTime derniereAction, LocalDateTime dateLead) {
        return derniereAction != null ? derniereAction : dateLead;
    }

    public static boolean endormi(LocalDateTime reference, LocalDateTime maintenant) {
        return reference.isBefore(seuil(maintenant));
    }

    /** Jours entiers écoulés ; jamais négatif (une date future compte pour 0). */
    public static int joursSansAction(LocalDateTime reference, LocalDateTime maintenant) {
        return (int) Math.max(0L, ChronoUnit.DAYS.between(reference, maintenant));
    }
}
