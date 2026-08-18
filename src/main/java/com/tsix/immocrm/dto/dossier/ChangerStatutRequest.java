package com.tsix.immocrm.dto.dossier;

/**
 * Body for {@code PATCH /api/dossiers/{id}/statut}. {@code statut} must be a
 * {@link com.tsix.immocrm.domain.StatutDossier} code (e.g. "FORMALISE", "ENVOI AU NOTAIRE").
 * {@code note} is optional free text recorded on the Dossier ({@code ComFolder.Notes}) when
 * provided; existing notes are overwritten, not appended.
 */
public record ChangerStatutRequest(
        String statut,
        String note
) {
}
