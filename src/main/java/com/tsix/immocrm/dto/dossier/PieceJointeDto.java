package com.tsix.immocrm.dto.dossier;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metadata for one pièce jointe of a Dossier ({@code AdmAttachedFile}, see
 * {@code AdmAttachedFileSummary} for the confirmed linkage). The binary content is fetched
 * separately via {@code GET /api/dossiers/{id}/pieces-jointes/{pieceId}/fichier} — never
 * inlined here.
 */
public record PieceJointeDto(
        UUID id,
        String description,
        String extension,
        LocalDateTime dateCreation
) {
}
