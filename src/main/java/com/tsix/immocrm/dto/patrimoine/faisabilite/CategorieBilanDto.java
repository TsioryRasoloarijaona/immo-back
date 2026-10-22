package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Catégorie de trésorerie sélectionnable dans le bilan (StlCategory hors « sans prévision »).")
public record CategorieBilanDto(
        @Schema(description = "PKey.") UUID id,
        @Schema(description = "Code.") String code,
        @Schema(description = "Libellé.") String libelle,
        @Schema(description = "Catégorie parente ; null pour une racine.") UUID parentId,
        @Schema(description = "Côté recette (IsCredit).") boolean estRecette,
        @Schema(description = "Ordre d'affichage (Sorting).") Integer ordre) {
}
