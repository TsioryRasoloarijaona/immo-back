package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Saisie d'un rendez-vous. En modification, champ absent ou null = inchangé. Code et source n'en font pas partie.")
public record RendezVousEcritureDto(
        @Schema(description = "Obligatoire à la création, 255 caractères au plus.") String description,
        @Schema(description = "Déclencheur (CfgTier) ; inconnu = 400.") UUID declencheurId,
        @Schema(description = "Obligatoire à la création.") LocalDateTime datePrevue,
        @Schema(description = "Durée prévisionnelle.") Integer dureePrevue,
        @Schema(description = "Absente à la création = datePrevue ; antérieure à datePrevue = 400.") LocalDateTime dateFinPrevue,
        @Schema(description = "Personnel (CfgTier) ; inconnu = 400.") UUID gestionnaireId,
        @Schema(description = "Tier (CfgTier), obligatoire à la création ; inconnu = 400.") UUID tierId,
        @Schema(description = "Notes, 1000 caractères au plus.") String notes) {
}
