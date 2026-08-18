package com.tsix.immocrm.dto.prospection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shape for entries in {@code GET /api/prospections/{id}/interactions} (journal, backed by
 * {@code ComActionMessage}).
 */
public record InteractionDto(
        UUID id,
        LocalDateTime date,
        String description,
        Boolean entrante,
        UUID employeId,
        String employeNom
) {
}
