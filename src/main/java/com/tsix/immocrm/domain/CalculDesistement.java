package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.util.Objects.requireNonNullElse;

/** Pénalité et remboursement d'un désistement (FEATURE-desistements.md R3-R5, {@code SaleWithdrawalCancellation.cs}). */
public final class CalculDesistement {

    private static final int ECHELLE_REMISE = 6;

    private CalculDesistement() {
    }

    /** Taux et remise en fraction ; {@code resteFrais} = reste dû des frais « toujours payés ». */
    public record Entree(BigDecimal taux, boolean basePrix, BigDecimal prix, BigDecimal totalRegle, BigDecimal remise,
                         BigDecimal resteFrais) {

        public Entree avecRemise(BigDecimal valeur) {
            return new Entree(taux, basePrix, prix, totalRegle, valeur, resteFrais);
        }

        private BigDecimal penaliteBrute() {
            return nonNul(taux).multiply(nonNul(basePrix ? prix : totalRegle));
        }
    }

    /** taux × base − taux × base × remise, au centime. */
    public static BigDecimal penaliteApresRemise(Entree e) {
        BigDecimal brute = e.penaliteBrute();
        return brute.subtract(brute.multiply(nonNul(e.remise()))).setScale(2, RoundingMode.HALF_UP);
    }

    /** max(0, total réglé − pénalité après remise − reste frais à régler). */
    public static BigDecimal aRembourser(Entree e) {
        return nonNul(e.totalRegle()).subtract(penaliteApresRemise(e)).subtract(nonNul(e.resteFrais()))
                .max(BigDecimal.ZERO);
    }

    /** Remise qui donne la pénalité saisie (setter {@code TransfertExpenseAmountAfterDiscount}) ; 0 si taux × base nul. */
    public static BigDecimal remisePour(BigDecimal penalite, Entree e) {
        BigDecimal brute = e.penaliteBrute();
        if (brute.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE.subtract(penalite.divide(brute, ECHELLE_REMISE, RoundingMode.HALF_UP));
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return requireNonNullElse(valeur, BigDecimal.ZERO);
    }
}
