package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/**
 * {@code ComProspectionProject} row — the "Autres tranches" tab: a secondary tranche/article
 * interest, independent from the prospection's tranche/article principal (see
 * {@code ProspectionDetailDto.trancheId}/{@code articleId} and
 * {@code PATCH /api/prospections/{id}/tranche-principale}).
 */
public record ProspectionAutreTrancheDto(
        UUID id,
        UUID trancheId,
        String tranche,
        String trancheDescription,
        UUID articleId,
        String article,
        String articleDescription
) {
}
