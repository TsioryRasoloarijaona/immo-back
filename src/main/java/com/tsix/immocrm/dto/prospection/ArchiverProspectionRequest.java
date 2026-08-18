package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/**
 * Body for {@code PATCH /api/prospections/{id}/archiver}. At least one of
 * {@code raisonId}/{@code raisonTexte} is required ("Archive avec raison").
 */
public record ArchiverProspectionRequest(
        UUID raisonId,
        String raisonTexte
) {
}
