package com.tsix.immocrm.dto.patrimoine;

import java.time.LocalDateTime;
import java.util.UUID;

public record TrancheListItemDto(
        UUID id,
        String code,
        String description,
        String ville,
        LocalDateTime dateLivraison
) {
}
