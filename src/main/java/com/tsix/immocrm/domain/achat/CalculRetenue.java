package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Seul calcul d'une ligne de retenue à la source (voir FEATURE-n3bis-b2a-retenue.md §6 RS-a à RS-d). */
public final class CalculRetenue {

    // Le taux est stocké en decimal(26,6) : arrondi à la colonne, pas à la devise.
    private static final int DECIMALES_TAUX = 6;

    private CalculRetenue() {
    }

    /** Montant RS = base × taux, arrondi à la devise. */
    public static BigDecimal montantRs(BigDecimal base, BigDecimal taux) {
        return CalculReglement.arrondi(nonNul(base).multiply(nonNul(taux)));
    }

    /** Taux = montant RS / base ; 0 si la base est nulle (RS-c, RS-d). */
    public static BigDecimal taux(BigDecimal base, BigDecimal montantRs) {
        if (CalculReglement.estNul(base)) {
            return BigDecimal.ZERO;
        }
        return nonNul(montantRs).divide(base, DECIMALES_TAUX, RoundingMode.HALF_UP);
    }

    /** Net = base − montant RS, arrondi à la devise. */
    public static BigDecimal net(BigDecimal base, BigDecimal montantRs) {
        return CalculReglement.arrondi(CalculReglement.resteAAffecter(base, montantRs));
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
