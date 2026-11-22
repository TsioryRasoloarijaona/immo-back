package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

/** Seul calcul du montant affecté et du reste à affecter d'un règlement (voir FEATURE-n3bis-saisie-reglements.md D3). */
public final class CalculReglement {

    private static final int DECIMALES_DEVISE = 2;

    private CalculReglement() {
    }

    /** Σ des montants des lignes, lignes masquées comprises ; une ligne sans montant compte 0. */
    public static BigDecimal affecte(Collection<BigDecimal> lignes) {
        return lignes.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Montant du règlement (base) − affecté. */
    public static BigDecimal resteAAffecter(BigDecimal montant, BigDecimal affecte) {
        return nonNul(montant).subtract(nonNul(affecte));
    }

    /** Vrai si le reste à affecter, arrondi à la devise, est négatif. */
    public static boolean depasse(BigDecimal montant, BigDecimal affecte) {
        return arrondi(resteAAffecter(montant, affecte)).signum() < 0;
    }

    /** Vrai si le montant, arrondi à la devise, est nul (ligne à 0). */
    public static boolean estNul(BigDecimal montant) {
        return arrondi(nonNul(montant)).signum() == 0;
    }

    public static BigDecimal arrondi(BigDecimal montant) {
        return montant.setScale(DECIMALES_DEVISE, RoundingMode.HALF_UP);
    }

    private static BigDecimal nonNul(BigDecimal montant) {
        return montant == null ? BigDecimal.ZERO : montant;
    }
}
