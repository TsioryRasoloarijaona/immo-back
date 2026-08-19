package com.tsix.immocrm.dto.prospect;

import java.time.LocalDateTime;
import java.util.UUID;

public record PieceIdentiteRequest(
        UUID typeId,
        String numero,
        LocalDateTime delivreeLe,
        String lieuDelivrance,
        LocalDateTime valideJusquau,
        String lieuValidite
) {
}
