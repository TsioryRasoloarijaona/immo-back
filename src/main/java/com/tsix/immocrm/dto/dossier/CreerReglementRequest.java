package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Body for {@code POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements}. */
public record CreerReglementRequest(
        BigDecimal montant,
        LocalDateTime dateReglement,
        UUID typeId,
        UUID banqueId,
        String notes
) {
}
