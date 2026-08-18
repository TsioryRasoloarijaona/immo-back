package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/**
 * Body for {@code PATCH /api/prospections/{id}/etape}: changes the prospection's
 * "Nature" ({@code ComProspectionKind}). Only validated for membership (the referenced
 * kind must exist) — no strict transition table, per the endpoint spec.
 * {@code note} is optional free text recorded on the Prospection ({@code ComProspection.Notes})
 * when provided; existing notes are overwritten, not appended.
 */
public record ChangerEtapeRequest(
        UUID etapeId,
        String note
) {
}
