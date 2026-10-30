package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Saisie d'une tâche. En modification, champ absent ou null = inchangé. Code et statut n'en font pas partie.")
public record TacheEcritureDto(
        @Schema(description = "Obligatoire à la création, 255 caractères au plus.") String description,
        @Schema(description = "Type de tâche (AdmTaskType) ; inconnu = 400.") UUID typeId,
        @Schema(description = "Tiers déclencheur (CfgTier) ; inconnu = 400.") UUID declencheurId,
        @Schema(description = "Date prévisionnelle.") LocalDateTime datePrevue,
        @Schema(description = "Durée prévisionnelle.") Integer dureePrevue,
        @Schema(description = "Date de fin prévisionnelle, jamais calculée.") LocalDateTime dateFinPrevue,
        @Schema(description = "Date de début réelle.") LocalDateTime dateDebut,
        @Schema(description = "Durée réelle.") Integer duree,
        @Schema(description = "Date de fin réelle.") LocalDateTime dateFin,
        @Schema(description = "Module (AdmModule) ; inconnu = 400.") UUID moduleId,
        @Schema(description = "Priorité (AdmTaskPriority) ; inconnue = 400.") UUID prioriteId,
        @Schema(description = "Tiers gestionnaire (CfgTier) ; inconnu = 400.") UUID gestionnaireId,
        @Schema(description = "Tiers assigné (CfgTier) ; inconnu = 400.") UUID assigneId,
        @Schema(description = "Entité (CfgCompany) ; inconnue = 400.") UUID entiteId,
        @Schema(description = "Projet (PrjProject) ; inconnu = 400.") UUID projetId,
        @Schema(description = "Notes, 1000 caractères au plus.") String notes) {
}
