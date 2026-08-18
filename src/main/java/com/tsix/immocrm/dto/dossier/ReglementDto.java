package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Shape for {@code POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements} (backed by {@code StlSettlement}). */
public record ReglementDto(
        UUID id,
        UUID echeanceId,
        LocalDateTime dateDocument,
        LocalDateTime dateReglement,
        BigDecimal montant,
        String type,
        String notes
) {
}
