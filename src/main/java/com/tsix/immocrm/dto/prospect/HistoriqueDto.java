package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** Une ligne de l'onglet « Historique » d'une fiche (prospect, prospection). */
public record HistoriqueDto(
        UUID id,

        @Schema(description = "Date de l'événement.")
        LocalDateTime date,

        String description,

        @Schema(description = "Auteur de l'événement (libellé du compte). Null sur les lignes legacy sans auteur.")
        String utilisateur,

        @Schema(description = "Libellé du workflow (historique de prospection). Toujours null pour un tiers.")
        String workflowDescription,

        String statutDescription,

        @Schema(description = "Dossier de vente concerné (historique d'un tiers). Null pour une prospection.")
        UUID dossierId,

        @Schema(description = "N° du dossier de vente, ex. « ARS 2026/00410 ». Null pour une prospection.")
        String dossierCode,

        @Schema(description = "Code de l'article principal du dossier. Null pour une prospection ou sans article.")
        String articleCode
) {
}
