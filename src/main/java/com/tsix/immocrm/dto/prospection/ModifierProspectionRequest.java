package com.tsix.immocrm.dto.prospection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code PATCH /api/prospections/{id}} body — edits qualification fields (origine, vocation,
 * budget, notes...). All fields optional/partial: {@code null} means "leave unchanged", matching
 * the {@code applyFields}-style partial-update convention used elsewhere (e.g.
 * {@code ModifierProspectRequest}, {@code ModifierEcheanceRequest}).
 *
 * <p>Tranche/article principaux ne sont volontairement PAS ici — voir
 * {@link ModifierTranchePrincipaleRequest} / {@code PATCH /api/prospections/{id}/tranche-principale},
 * le seul endpoint qui les modifie, avec la validation de cohérence article↔tranche que ce endpoint
 * générique ne faisait pas.
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
