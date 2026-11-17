package com.tsix.immocrm.domain.projet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/** Montants d'un avenant (FEATURE-lot-avenants-avances.md R4-R5) : seul calcul, ratios 0–1, aucun arrondi des sommes. */
public final class CalculAvenant {

    private static final int ECHELLE = 6;

    private CalculAvenant() {
    }

    /** Ligne d'avenant ; PU TTC = fonction du bordereau. */
    public record Ligne(BigDecimal quantite, BigDecimal prixHT, BigDecimal fodecRatio, BigDecimal tvaRatio) {
        public BigDecimal prixTTC() {
            return MontantsLot.prixTTC(prixHT, fodecRatio, tvaRatio);
        }

        public BigDecimal totalHT() {
            return z(quantite).multiply(z(prixHT)).multiply(BigDecimal.ONE.add(z(fodecRatio)));
        }

        public BigDecimal totalTTC() {
            return z(quantite).multiply(prixTTC());
        }
    }

    /** Retenue au taux ({@code ratio}) ou au montant ({@code parMontant}) ; l'autre valeur est déduite par {@link #calculer}. */
    public record Retenue(boolean parMontant, BigDecimal ratio, BigDecimal montant) {
    }

    /** Retenues dans l'ordre reçu, résolues (ratio et montant cohérents) ; net = TTC − Σ retenues. */
    public record Totaux(BigDecimal ht, BigDecimal tva, BigDecimal ttc, List<Retenue> retenues, BigDecimal net) {
    }

    public static Totaux calculer(List<Ligne> lignes, List<Retenue> retenues) {
        BigDecimal ht = lignes.stream().map(Ligne::totalHT).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ttc = lignes.stream().map(Ligne::totalTTC).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Retenue> resolues = retenues.stream().map(r -> resoudre(r, ttc)).toList();
        BigDecimal net = resolues.stream().map(Retenue::montant).reduce(ttc, BigDecimal::subtract);
        return new Totaux(ht, ttc.subtract(ht), ttc, resolues, net);
    }

    private static Retenue resoudre(Retenue r, BigDecimal ttc) {
        if (r.parMontant()) {
            BigDecimal montant = z(r.montant());
            BigDecimal ratio = ttc.signum() == 0 ? BigDecimal.ZERO : montant.divide(ttc, ECHELLE, RoundingMode.HALF_UP);
            return new Retenue(true, ratio, montant);
        }
        BigDecimal ratio = z(r.ratio());
        return new Retenue(false, ratio, ratio.multiply(ttc).setScale(ECHELLE, RoundingMode.HALF_UP));
    }

    private static BigDecimal z(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }
}
