package com.tsix.immocrm.dto.prospection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code PATCH /api/prospections/{id}} body — edits qualification fields (origine, vocation,
 * budget, tranche/article, notes...). All fields optional/partial: {@code null} means "leave
 * unchanged", matching the {@code applyFields}-style partial-update convention used elsewhere
 * (e.g. {@code ModifierProspectRequest}, {@code ModifierEcheanceRequest}).
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
        String notes,
        UUID trancheId,
        UUID articleId
) {
}
