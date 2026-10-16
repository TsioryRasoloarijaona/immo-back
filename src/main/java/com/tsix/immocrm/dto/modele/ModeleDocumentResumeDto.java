package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Modèle actif proposé dans « Imprimer » d'une fiche.")
public record ModeleDocumentResumeDto(
        @Schema(description = "Identifiant du modèle (AppModeleDocument.PKey).")
        UUID id,
        @Schema(description = "Code du modèle, unique dans sa catégorie.", example = "ACTE_RESERVATION")
        String code,
        @Schema(description = "Nom affiché.", example = "Acte de réservation")
        String libelle) {
}
