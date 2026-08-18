package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/**
 * Body for {@code POST /api/prospections/{id}/transformer}: assigns the commercial that
 * completes a pré-prospection into a full prospection (the prospect itself is created
 * separately via {@code POST /api/pre-prospections/{id}/creer-prospect}).
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo).
 */
public record TransformerProspectionRequest(
        UUID commercialId
) {
}
