package com.tsix.immocrm.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Chevauchement des périodes de deux lignes de commission de même clé, conditions exactes de {@code CfgTranche.Verif}
 * (parité OMRAN, fin ouverte comprise : FEATURE-patrimoine-tranche-regles.md §2, §11.1).
 */
public final class ChevauchementCommissions {

    /** {@code cle} : personnel + action + commercial + catégorie, comparée par {@code equals} (null = null). */
    public record Periode(Object cle, LocalDateTime debut, LocalDateTime fin) {
    }

    private ChevauchementCommissions() {
    }

    public static boolean present(List<Periode> periodes) {
        for (int i = 0; i < periodes.size(); i++) {
            for (int j = 0; j < periodes.size(); j++) {
                if (i != j && chevauchent(periodes.get(i), periodes.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean chevauchent(Periode l, Periode a) {
        return Objects.equals(l.cle(), a.cle()) && (dansLaPeriode(l, a) || avantUneBorneOuverte(l, a));
    }

    /** Début ou fin de {@code l} dans la période fermée de {@code a}. */
    private static boolean dansLaPeriode(Periode l, Periode a) {
        return entre(l.debut(), a) || entre(l.fin(), a);
    }

    private static boolean entre(LocalDateTime d, Periode a) {
        return inferieur(a.debut(), d) && inferieur(d, a.fin());
    }

    /** Début ou fin de {@code l} avant le début d'une période sans fin, ou avant la fin d'une période sans début. */
    private static boolean avantUneBorneOuverte(Periode l, Periode a) {
        if (a.fin() == null && (inferieur(l.debut(), a.debut()) || inferieur(l.fin(), a.debut()))) {
            return true;
        }
        return a.debut() == null && (inferieur(l.debut(), a.fin()) || inferieur(l.fin(), a.fin()));
    }

    /** {@code a <= b}, faux si l'une est nulle (comparaison levée de .NET). */
    private static boolean inferieur(LocalDateTime a, LocalDateTime b) {
        return a != null && b != null && !a.isAfter(b);
    }
}
