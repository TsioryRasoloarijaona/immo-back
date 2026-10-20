package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** {@code AdmAttachedFile} (liaison : {@code AdmAttachedFileSummary}) ; le contenu se lit par {@code GET .../fichier}. */
@Schema(description = "Métadonnées d'une pièce jointe rattachée à un dossier.")
public record PieceJointeDto(
        @Schema(description = "Identifiant de la pièce jointe (AdmAttachedFile.PKey).")
        UUID id,
        @Schema(description = "Légende/description libre de la pièce jointe (ex. \"CIN client\", \"Contrat signé\").")
        String description,
        String category,
        String type,
        @Schema(description = "Extension du fichier, avec le point (ex. \".pdf\").")
        String extension,
        LocalDateTime dateCreation,
        String creePar,
        @Schema(description = "Groupe libre de la pièce (AdmAttachedFile.FoderGroup, ex. « PLAN ») ; null si aucun.")
        String groupe
) {
}
