package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse de {@code POST /api/cles-api} et de {@code POST /api/cles-api/{id}/regenerer} : la
 * seule occasion de lire le secret. {@code instructions/FEATURE-cle-api-n8n.md} §5.
 */
@Schema(description = "Clé créée ou régénérée. `cle` est le secret **en clair, montré une seule "
        + "fois** : il n'est pas stocké (seul son hash l'est) et aucune autre route ne le renvoie. "
        + "Le copier immédiatement dans la configuration du client (n8n : credential Header Auth, "
        + "en-tête `X-Api-Key`).")
public record CleApiCreeeDto(
        @Schema(description = "La clé, sous la même forme que dans la liste.") CleApiDto cleApi,
        @Schema(description = "Le secret complet, `ak_` + 43 caractères. À envoyer dans `X-Api-Key`.",
                example = "ak_3fZk9qLmXw8vR2tYpN6bH1cJ4dKsA7eF0gUiO5lZqWm")
        String cle
) {
}
