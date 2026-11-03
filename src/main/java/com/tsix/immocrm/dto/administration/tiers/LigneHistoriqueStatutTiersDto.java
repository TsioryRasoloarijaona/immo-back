package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Changement de statut de dossier rattaché au tiers (StrFolderStatusHistory.CfgTierId).")
public record LigneHistoriqueStatutTiersDto(
        @Schema(description = "PKey de la ligne d'historique.") UUID id,
        @Schema(description = "Date (DocDate), clé du tri.") LocalDateTime date,
        @Schema(description = "Libellé du statut.") String statut,
        @Schema(description = "Code de l'article.") String article,
        @Schema(description = "Détail (Description).") String detail,
        @Schema(description = "Code du dossier de vente, null pour un autre dossier.") String dossierCode,
        @Schema(description = "Libellé de l'utilisateur.") String utilisateur) implements LigneOngletTiers {
}
