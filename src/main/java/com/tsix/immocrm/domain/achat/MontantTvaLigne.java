package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Montants d'une ligne de l'état de la TVA déductible (FEATURE-n7-etats-achat.md R1, R2), arrondis à 2 décimales. */
public record MontantTvaLigne(BigDecimal montantHT, BigDecimal tauxTvaPct, BigDecimal montantTva) {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    /** R1 : HT = (PU − PU × remise) × quantité, TVA = HT × taux ; remise et taux en fractions. */
    public static MontantTvaLigne deLigne(BigDecimal quantite, BigDecimal prixUnitaireHT, BigDecimal remise,
                                          BigDecimal taux) {
        BigDecimal pu = zero(prixUnitaireHT);
        BigDecimal ht = pu.subtract(pu.multiply(zero(remise))).multiply(zero(quantite));
        return new MontantTvaLigne(arrondi(ht), zero(taux).multiply(CENT), arrondi(ht.multiply(zero(taux))));
    }

    /** R2 : facture sans ligne, en-tête lu tel quel ; taux = TVA / HT (0 si HT nul). */
    public static MontantTvaLigne deEnTete(BigDecimal ht, BigDecimal tva) {
        BigDecimal base = zero(ht);
        BigDecimal taux = base.signum() == 0 ? BigDecimal.ZERO
                : zero(tva).multiply(CENT).divide(base, 4, RoundingMode.HALF_UP);
        return new MontantTvaLigne(arrondi(base), taux, arrondi(zero(tva)));
    }

    private static BigDecimal arrondi(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
