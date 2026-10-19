package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne de l'onglet Finalisations (CfgTrancheFolderEntity) : une par type de dossier.")
public record LigneFinalisationDto(
        @Schema(description = "PKey de la ligne ; absent au PUT = nouvelle ligne.") UUID id,
        @Schema(description = "Type de dossier (StrEntity), obligatoire et unique ; choix : /api/tranches/entites-finalisation.")
        UUID entiteDossierId,
        @Schema(description = "Statut de finalisation 1, obligatoire, statut du type de dossier choisi.")
        UUID statutFinalisation1Id,
        @Schema(description = "Statut de finalisation 2, facultatif, statut du type de dossier choisi.")
        UUID statutFinalisation2Id
) implements LignesTrancheDto.Ligne {
}
