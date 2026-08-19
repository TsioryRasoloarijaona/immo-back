package com.tsix.immocrm.dto.dossier;

import java.time.LocalDateTime;

/**
 * Body for {@code PATCH /api/dossiers/{id}/statut}. {@code statut} must be a
 * {@link com.tsix.immocrm.domain.StatutDossier} code (e.g. "FORMALISE", "ENVOI AU NOTAIRE").
 *
 * {@code date} is optional and, when the target status has a confirmed "milestone" column (see
 * {@link com.tsix.immocrm.domain.EtapeMilestone}, e.g. {@code AgreementSavingDate} for MINUTE),
 * is written onto that column. Ignored for statuses with no milestone column.
 *
 * {@code note} is optional free text: recorded on {@code ComFolder.Notes} for most transitions,
 * except {@code ANNULATION} where it's recorded as {@code ComFolder.CancelDescription} instead
 * (the cause of cancellation — confirmed real usage, e.g. "faute de frappe, changement appt").
 */
public record ChangerStatutRequest(
        String statut,
        LocalDateTime date,
        String note
) {
}
