package com.tsix.immocrm.domain.faisabilite;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/** Échéancier et remboursements d'un crédit d'étude ({@code FSFunding.GenerateLoanSchedules}, {@code FSReimbursement}). */
public final class FinancementFaisabilite {

    private static final MathContext MC = MathContext.DECIMAL128;

    private FinancementFaisabilite() {
    }

    /** Remboursement : à régler = base + intérêts. */
    public record Echeance(LocalDateTime date, BigDecimal base, BigDecimal taux, BigDecimal interets, BigDecimal aRegler) {
    }

    /** Périodicité OMRAN : toute valeur &lt; 1 devient 1. */
    public static int pas(Integer periodicite) {
        return periodicite == null || periodicite < 1 ? 1 : periodicite;
    }

    /** {@code AddMonths} depuis la première date (déblocage + grâce) : jour borné au dernier du mois, sans dérive. */
    public static List<Echeance> echeancier(LocalDateTime deblocage, BigDecimal montant, int nombre, Integer periodicite,
                                            Integer grace, BigDecimal taux) {
        LocalDateTime premiere = deblocage.plusMonths(grace == null ? 0 : grace);
        BigDecimal total = montant == null ? BigDecimal.ZERO : montant;
        BigDecimal base = arrondi(total.divide(BigDecimal.valueOf(nombre), MC));
        // Invariant I2 : Σ base = déblocage à l'échelle stockée ; la dernière échéance absorbe le reliquat d'arrondi.
        BigDecimal derniere = arrondi(total.subtract(base.multiply(BigDecimal.valueOf(nombre - 1L))));
        int pas = pas(periodicite);
        return IntStream.range(0, nombre)
                .mapToObj(i -> echeance(premiere.plusMonths((long) i * pas), i == nombre - 1 ? derniere : base, taux, null))
                .toList();
    }

    /** Saisie à deux sens : le taux gagne ; intérêts seuls → taux = intérêts / base (base ≠ 0). */
    public static Echeance echeance(LocalDateTime date, BigDecimal base, BigDecimal taux, BigDecimal interets) {
        BigDecimal b = base == null ? BigDecimal.ZERO : base;
        BigDecimal t;
        BigDecimal i;
        if (taux != null || interets == null) {
            t = taux == null ? BigDecimal.ZERO : taux;
            i = b.multiply(t, MC);
        } else {
            i = interets;
            t = b.signum() == 0 ? BigDecimal.ZERO : interets.divide(b, MC);
        }
        return new Echeance(date, b, t, i, b.add(i, MC));
    }

    private static BigDecimal arrondi(BigDecimal v) {
        return v.setScale(6, RoundingMode.HALF_UP);
    }
}
