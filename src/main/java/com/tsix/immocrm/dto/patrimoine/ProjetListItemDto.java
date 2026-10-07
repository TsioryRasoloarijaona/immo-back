package com.tsix.immocrm.dto.patrimoine;

import java.util.UUID;

public record ProjetListItemDto(
        UUID id,
        String code,
        String description,
        String ville
) {
}
