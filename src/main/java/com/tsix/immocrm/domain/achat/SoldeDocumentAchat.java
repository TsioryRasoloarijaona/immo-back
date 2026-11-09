package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Solde d'une pièce d'achat ({@code BuyDocument}) : reste = {@code AmountToPay} − Σ lignes de règlement non annulées.
 * Le réglé est la somme brute de {@code StlSettlementLine.Amount}, jamais {@code SQL_MONTANT_SIGNE} (IsCredit = 0 l'inverserait).
 * Partagé par Projet et Achat (règle 6) — voir FEATURE-achat-lecture-factures.md §2.
 */
public record SoldeDocumentAchat(BigDecimal montantAPayer, BigDecimal montantRegle, BigDecimal resteARegler, boolean solde) {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.05");

    public static SoldeDocumentAchat de(BigDecimal montantAPayer, BigDecimal montantRegle) {
        BigDecimal aPayer = arrondi(montantAPayer);
        BigDecimal regle = arrondi(montantRegle);
        BigDecimal reste = aPayer.subtract(regle);
        return new SoldeDocumentAchat(aPayer, regle, reste, reste.abs().compareTo(TOLERANCE) <= 0);
    }

    private static BigDecimal arrondi(BigDecimal montant) {
        return (montant == null ? BigDecimal.ZERO : montant).setScale(2, RoundingMode.HALF_UP);
    }
}
