package com.tsix.immocrm.domain.chantier;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Seul calcul « facturé / réglé / reste » (décompte, lot, projet). Facturé = {@code BuyDocument.AmountToPay}
 * (régularisation comprise, pas le net du décompte) ; réglé = lignes de règlement non annulées
 * ({@code StatutReglement}) ; un ensemble de factures se solde par somme des soldes unitaires.
 */
public record SoldeFacture(BigDecimal totalFacture, BigDecimal totalRegle, BigDecimal resteARegler) {

    public static final SoldeFacture AUCUNE = new SoldeFacture(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public static SoldeFacture de(BigDecimal netAPayerFacture, BigDecimal regleNonAnnule) {
        BigDecimal facture = Montants.arrondi(netAPayerFacture);
        BigDecimal regle = Montants.arrondi(regleNonAnnule);
        return new SoldeFacture(facture, regle, facture.subtract(regle));
    }

    public SoldeFacture plus(SoldeFacture autre) {
        return new SoldeFacture(totalFacture.add(autre.totalFacture), totalRegle.add(autre.totalRegle),
                resteARegler.add(autre.resteARegler));
    }

    public static SoldeFacture somme(Collection<SoldeFacture> soldes) {
        return soldes.stream().reduce(AUCUNE, SoldeFacture::plus);
    }
}
