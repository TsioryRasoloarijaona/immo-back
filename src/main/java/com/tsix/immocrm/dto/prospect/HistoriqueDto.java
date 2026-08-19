package com.tsix.immocrm.dto.prospect;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code StrHistory} filtered by ObjectId — feeds the "Historique" tab present on every Omran
 * fiche (prospect, prospection, dossier).
 */
public record HistoriqueDto(
        UUID id,
        LocalDateTime date,
        String description,
        String utilisateur,
        String workflowDescription,
        String statutDescription
) {
}
