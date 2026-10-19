package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Échéance que « Mettre à jour les schémas de financement » déplace au début des finalisations.")
public record EcheanceSchemaDto(
        @Schema(description = "PKey de l'échéance (ComDocument).") UUID echeanceId,
        @Schema(description = "Dossier courant de l'article (StkItem.ComFolderId).") UUID dossierId,
        @Schema(description = "Code du dossier.") String dossierCode,
        @Schema(description = "Client du dossier.") String client,
        @Schema(description = "Catégorie de l'échéance.") String categorie,
        @Schema(description = "Date actuelle de l'échéance (DocDate).") LocalDateTime ancienneDate,
        @Schema(description = "Date après mise à jour : début des finalisations de la tranche.") LocalDateTime nouvelleDate,
        @Schema(description = "Reste dû calculé : montant − règlements non annulés ; toujours > 0 ici.") BigDecimal reste
) {
}
