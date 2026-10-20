package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.util.Objects.requireNonNullElse;

/** Pénalité et remboursement d'un désistement (FEATURE-desistements.md R3-R5, {@code SaleWithdrawalCancellation.cs}). */
public final class CalculDesistement {

    private static final int ECHELLE_REMISE = 6;

    private CalculDesistement() {
    }

    /**
     * Taux et remise en fraction ; {@code resteFrais} = reste dû des frais « toujours payés » ; {@code penaliteSaisie}
     * non nulle = pénalité saisie, gardée telle quelle comme OMRAN (la remise stockée n'a que 6 décimales).
     */
    public record Entree(BigDecimal taux, boolean basePrix, BigDecimal prix, BigDecimal totalRegle, BigDecimal remise,
                         BigDecimal resteFrais, BigDecimal penaliteSaisie) {

        public Entree(BigDecimal taux, boolean basePrix, BigDecimal prix, BigDecimal totalRegle, BigDecimal remise,
                      BigDecimal resteFrais) {
            this(taux, basePrix, prix, totalRegle, remise, resteFrais, null);
        }

        public Entree avecRemise(BigDecimal valeur) {
            return new Entree(taux, basePrix, prix, totalRegle, valeur, resteFrais, null);
        }

        /** Remise recalculée depuis la pénalité saisie, pénalité conservée au centime. */
        public Entree avecPenaliteSaisie(BigDecimal penalite) {
            return new Entree(taux, basePrix, prix, totalRegle, remisePour(penalite, this), resteFrais,
                    penalite.setScale(2, RoundingMode.HALF_UP));
        }

        /**
         * Pénalité enregistrée conservée tant qu'elle ne s'écarte de la formule que de l'arrondi de la remise ; un
         * changement de base (prix, réglé, taux) la fait recalculer, comme {@code RefrechAmounts}.
         */
        public Entree avecPenaliteStockee(BigDecimal stockee) {
            if (stockee == null) {
                return this;
            }
            BigDecimal tolerance = penaliteBrute().abs().multiply(TOLERANCE_REMISE).add(CENTIME);
            boolean arrondi = formule(this).subtract(stockee).abs().compareTo(tolerance) <= 0;
            return arrondi ? new Entree(taux, basePrix, prix, totalRegle, remise, resteFrais, stockee) : this;
        }

        private BigDecimal penaliteBrute() {
            return nonNul(taux).multiply(nonNul(basePrix ? prix : totalRegle));
        }
    }

    private static final BigDecimal TOLERANCE_REMISE = new BigDecimal("0.000001");
    private static final BigDecimal CENTIME = new BigDecimal("0.01");

    /** taux × base − taux × base × remise, au centime, sauf pénalité saisie. */
    public static BigDecimal penaliteApresRemise(Entree e) {
        return e.penaliteSaisie() != null ? e.penaliteSaisie() : formule(e);
    }

    private static BigDecimal formule(Entree e) {
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
