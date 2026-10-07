package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/** Voir instructions/FEATURE-cle-api-n8n.md §5. */
public record CleApiCreeeDto(
        @Schema(description = "La clé, sous la même forme que dans la liste.") CleApiDto cleApi,
        @Schema(description = "Le secret complet, `ak_` + 43 caractères. À envoyer dans `X-Api-Key`.",
                example = "ak_3fZk9qLmXw8vR2tYpN6bH1cJ4dKsA7eF0gUiO5lZqWm")
        String cle
) {
}
