package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Ligne du journal des actions (vue AdmActionsView).")
public record LigneActionDto(
        @Schema(description = "Clé de ligne seulement, <PKey>:<ActionLabel> : PKey seul n'est pas unique.") String id,
        @Schema(description = "Code de l'objet ; peut être null ou vide.") String code,
        @Schema(description = "Famille de l'objet (ActionLabel).") String action,
        @Schema(description = "Description de l'objet ; peut être null ou vide.") String description,
        @Schema(description = "Créateur ; peut être null ou vide.") String creePar,
        @Schema(description = "Date de création, ISO locale ; peut être null.", example = "2026-03-10T09:15:30") LocalDateTime creeLe,
        @Schema(description = "Dernier modificateur ; peut être null ou vide.") String modifiePar,
        @Schema(description = "Date de modification, ISO locale ; peut être null.", example = "2026-03-10T09:15:30") LocalDateTime modifieLe) {
}
