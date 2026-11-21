package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Saisie d'un modèle de planification ; en modification, un champ absent reste inchangé.")
public record PlanificationEcritureDto(
        @Schema(description = "Code unique, obligatoire à la création.") String code,
        @Schema(description = "Description, obligatoire à la création.") String description,
        @Schema(description = "Notes (1 000 caractères au plus).") String notes,
        @Schema(description = "Date de début ; absente à la création = aujourd'hui.") LocalDate dateDebut) {
}
