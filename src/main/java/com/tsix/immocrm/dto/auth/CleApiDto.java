package com.tsix.immocrm.dto.auth;

import com.tsix.immocrm.domain.PorteeCleApi;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Voir instructions/FEATURE-cle-api-n8n.md §5. */
public record CleApiDto(
        @Schema(description = "Identifiant (AppApiClient.PKey).") UUID id,
        @Schema(description = "Nom du client.", example = "n8n") String nom,
        @Schema(description = "Début de la clé, `ak_` + 8 caractères.", example = "ak_3fZk9qLm") String clePrefixe,
        @Schema(description = "Code du compte de service (`AdmUser.Code`).", example = "n8n") String compteCode,
        @Schema(description = "Nom affiché du compte de service — l'auteur des écritures de la clé.",
                example = "n8n")
        String compteNom,
        @Schema(description = "Portées accordées.") List<PorteeCleApi> portees,
        @Schema(description = "false = révoquée : la clé est refusée en 401.") boolean actif,
        @Schema(description = "Dernière requête acceptée avec cette clé, à la minute près ; null si jamais utilisée.")
        LocalDateTime dernierUsage,
        @Schema(description = "Date de création.") LocalDateTime dateCreation,
        @Schema(description = "Administrateur créateur.") String creePar,
        @Schema(description = "Date de révocation, null tant que la clé est active.") LocalDateTime revoqueLe,
        @Schema(description = "Administrateur ayant révoqué.") String revoquePar
) {
}
