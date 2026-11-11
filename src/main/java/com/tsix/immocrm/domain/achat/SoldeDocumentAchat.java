package com.tsix.immocrm.domain.achat;

import com.tsix.immocrm.domain.StatutReglement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Solde d'une pièce d'achat ({@code BuyDocument}) : reste = {@code AmountToPay} − Σ lignes de règlement non annulées.
 * Le réglé est la somme brute de {@code StlSettlementLine.Amount}, jamais {@code SQL_MONTANT_SIGNE} (IsCredit = 0 l'inverserait).
 * Seul calcul du solde d'achat (règle 6) — voir FEATURE-achat-impayes.md §3.1.
 */
public record SoldeDocumentAchat(BigDecimal montantAPayer, BigDecimal montantRegle, BigDecimal resteARegler, boolean solde) {

    /** Sous-requête scalaire du réglé d'une pièce, alias {@code d} = {@code BuyDocument} ; NULL sans ligne. */
    public static final String SQL_REGLE = "SELECT SUM(l.Amount) FROM StlSettlementLine l "
            + "JOIN StlSettlement s ON s.PKey = l.StlSettlementId WHERE l.BuyDocumentId = d.PKey AND "
            + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX;

    private static final BigDecimal TOLERANCE = new BigDecimal("0.05");
    private static final BigDecimal MOINS_TOLERANCE = TOLERANCE.negate();

    public static SoldeDocumentAchat de(BigDecimal montantAPayer, BigDecimal montantRegle) {
        BigDecimal aPayer = arrondi(montantAPayer);
        BigDecimal regle = arrondi(montantRegle);
        BigDecimal reste = aPayer.subtract(regle);
        return new SoldeDocumentAchat(aPayer, regle, reste, reste.abs().compareTo(TOLERANCE) <= 0);
    }

    /** Agrégat (fournisseur, liste) : recalculé sur les sommes, jamais la somme des restes arrondis à part. */
    public static SoldeDocumentAchat total(Collection<SoldeDocumentAchat> soldes) {
        return de(soldes.stream().map(SoldeDocumentAchat::montantAPayer).reduce(BigDecimal.ZERO, BigDecimal::add),
                soldes.stream().map(SoldeDocumentAchat::montantRegle).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Pièce négative (récupération d'avance sur décompte) : à déduire, jamais « trop réglée ». */
    public boolean aDeduire() {
        return montantAPayer.compareTo(MOINS_TOLERANCE) < 0;
    }

    public boolean tropRegle() {
        return !aDeduire() && resteARegler.compareTo(MOINS_TOLERANCE) < 0;
    }

    public boolean aRegler() {
        return resteARegler.compareTo(TOLERANCE) > 0;
    }

    private static BigDecimal arrondi(BigDecimal montant) {
        return (montant == null ? BigDecimal.ZERO : montant).setScale(2, RoundingMode.HALF_UP);
    }
}
