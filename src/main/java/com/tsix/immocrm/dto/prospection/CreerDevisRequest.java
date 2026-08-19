package com.tsix.immocrm.dto.prospection;

import java.math.BigDecimal;
import java.util.UUID;

public record CreerDevisRequest(
        UUID trancheId,
        UUID articleId,
        BigDecimal prix,
        String notes
) {
}
