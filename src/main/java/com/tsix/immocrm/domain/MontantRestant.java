package com.tsix.immocrm.domain;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNullElse;

/**
 * Seuls calculs de reste de l'échéancier et des règlements (FEATURE-reglement-multi-echeances.md §2) :
 * l'affichage, les contrôles d'écriture et la fiche encaissement passent tous par ici.
 */
public final class MontantRestant {

    private MontantRestant() {
    }

    /** Reste dû d'une échéance : montant − réglé signé hors CANCELED ({@code sumAmountByComDocumentId}). */
    public static BigDecimal echeance(BigDecimal montantEcheance, BigDecimal regle) {
        return difference(montantEcheance, regle);
    }

    /** Reste à affecter d'un règlement : {@code Amount} − part affectée ({@code StlSettlementAffectation}). */
    public static BigDecimal reglement(BigDecimal montantReglement, BigDecimal affecte) {
        return difference(montantReglement, affecte);
    }

    private static BigDecimal difference(BigDecimal montant, BigDecimal deduit) {
        return requireNonNullElse(montant, BigDecimal.ZERO).subtract(requireNonNullElse(deduit, BigDecimal.ZERO));
    }
}
