package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Une ligne de l'onglet Historique d'un article (StrFolderStatusHistory), la plus récente en premier.")
public record ArticleHistoriqueDto(
        @Schema(description = "StrFolderStatusHistory.PKey.")
        UUID id,
        @Schema(description = "Date du changement de statut (DocDate).")
        LocalDateTime date,
        String description,
        @Schema(description = "Auteur : libellé du compte ; « Système » pour une ligne de location ou de syndic sans compte.")
        String utilisateur,
        @Schema(description = "État atteint : libellé du statut.")
        String statutDescription,
        @Schema(description = "Dossier de vente concerné ; null pour une location ou un syndic.")
        UUID dossierId,
        @Schema(description = "N° du dossier de vente ; null pour une location ou un syndic.")
        String dossierCode,
        @Schema(description = "Code de l'article de la ligne (le sous-article pour une affectation).")
        String articleCode,
        @Schema(description = "Nature du dossier de la ligne ; null si la ligne ne vise aucun dossier.")
        OrigineDossier origine,
        @Schema(description = "Nom du client de la ligne (CfgTierId).")
        String client
) {
}
