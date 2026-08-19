package com.tsix.immocrm.dto.prospection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** {@code StlQuote} row — the "Offres" tab (GAP-07). Deliberately minimal for a first pass. */
public record DevisDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        UUID trancheId,
        String tranche,
        UUID articleId,
        String article,
        UUID prospectId,
        BigDecimal prix,
        String notes
) {
}
