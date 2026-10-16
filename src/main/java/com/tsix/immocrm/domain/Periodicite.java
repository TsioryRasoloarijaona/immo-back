package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Périodicités d'OMRAN ({@code RntPeriodicity} : M, B, T, S, A) et découpage d'un montant en échéances
 * ({@code ComPeriodicity.cs:GenerateLines}). Voir FEATURE-vente-echeancier.md §1.5.
 */
public enum Periodicite {
    MENSUELLE(1),
    BIMENSUELLE(2),
    TRIMESTRIELLE(3),
    SEMESTRIELLE(6),
    ANNUELLE(12);

    private static final int CENTIMES = 2;

    private final int mois;

    Periodicite(int mois) {
        this.mois = mois;
    }

    public int mois() {
        return mois;
    }

    public record Echeance(LocalDateTime date, BigDecimal montant) {
    }

    /**
     * Montant d'une période = total / nombre au centime, arrondi bancaire comme {@code Math.Round} ; la dernière
     * échéance porte le reste, la somme est donc exacte. Une échéance dont le reste tient dans une période arrête
     * la série (elle peut être nulle : l'appelant la retire). Dates : +{@code mois} cumulés depuis la première.
     */
    public List<Echeance> repartir(BigDecimal total, int nombre, LocalDateTime premiereDate) {
        BigDecimal parPeriode = total.divide(BigDecimal.valueOf(nombre), CENTIMES, RoundingMode.HALF_EVEN);
        List<Echeance> echeances = new ArrayList<>();
        BigDecimal cumul = BigDecimal.ZERO;
        LocalDateTime date = premiereDate;
        for (int i = 1; i <= nombre; i++) {
            BigDecimal reste = total.subtract(cumul);
            if (reste.compareTo(parPeriode) <= 0) {
                echeances.add(new Echeance(date, reste));
                break;
            }
            echeances.add(new Echeance(date, i == nombre ? reste : parPeriode));
            cumul = cumul.add(parPeriode);
            date = date.plusMonths(mois);
        }
        return echeances;
    }
}
