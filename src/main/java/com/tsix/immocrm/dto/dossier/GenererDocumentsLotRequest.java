package com.tsix.immocrm.dto.dossier;

import java.util.List;
import java.util.UUID;

/**
 * Body for every {@code POST /api/dossiers/documents/{type}} "génération en lot" endpoint —
 * select several dossiers (e.g. from a checkbox list on `DossiersList.tsx`) and get back one PDF
 * with each dossier's document concatenated in the order given, same content per dossier as the
 * single-dossier {@code GET /api/dossiers/{id}/documents/{type}} endpoint. Not offered for the
 * "Reçu de règlement" document (scoped to one règlement, not one dossier — no natural "select
 * several" action for it) nor for the two documents with a Word variant (Fiche/Acte de
 * réservation) — batch generation is PDF-only.
 */
public record GenererDocumentsLotRequest(
        List<UUID> dossierIds
) {
}
