package com.tsix.immocrm.dto.prospection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code null} = inchangé. Tranche/article principaux absents exprès : seul
 * {@link ModifierTranchePrincipaleRequest} les modifie, avec la validation article↔tranche.
 */
public record ModifierProspectionRequest(
        UUID origineId,
        UUID origineSupportId,
        UUID vocationId,
        UUID typeContactId,
        UUID actionCommercialeId,
        BigDecimal surface,
        BigDecimal surfaceJusqua,
        BigDecimal prix,
        BigDecimal prixJusqua,
        BigDecimal avance,
        BigDecimal financementFondsPropres,
        BigDecimal financementCredit,
        BigDecimal financementAutre,
        String notes
) {
}
