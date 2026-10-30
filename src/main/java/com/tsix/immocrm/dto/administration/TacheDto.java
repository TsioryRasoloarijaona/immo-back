package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Tâche d'administration (AdmTask de l'entité AdmTask, jamais un rendez-vous).")
public record TacheDto(
        @Schema(description = "PKey de la tâche.") UUID id,
        @Schema(description = "Code issu de la séquence AdmTask, jamais saisi.", example = "26/00110") String code,
        @Schema(description = "Description, obligatoire.") String description,
        @Schema(description = "Type de tâche (AdmTaskType), null si non renseigné.") UUID typeId,
        @Schema(description = "Libellé du type.") String typeLibelle,
        @Schema(description = "Tiers déclencheur (CfgTier), null si non renseigné.") UUID declencheurId,
        @Schema(description = "Nom du déclencheur.") String declencheurNom,
        @Schema(description = "Date prévisionnelle, clé de tri de la liste.") LocalDateTime datePrevue,
        @Schema(description = "Durée prévisionnelle (entier, unité d'OMRAN).") Integer dureePrevue,
        @Schema(description = "Date de fin prévisionnelle, saisie et jamais calculée.") LocalDateTime dateFinPrevue,
        @Schema(description = "Date de début réelle.") LocalDateTime dateDebut,
        @Schema(description = "Durée réelle (entier, unité d'OMRAN).") Integer duree,
        @Schema(description = "Date de fin réelle.") LocalDateTime dateFin,
        @Schema(description = "Module (AdmModule), null si non renseigné.") UUID moduleId,
        @Schema(description = "Priorité (AdmTaskPriority), null si non renseignée.") UUID prioriteId,
        @Schema(description = "Libellé de la priorité.") String prioriteLibelle,
        @Schema(description = "Tiers gestionnaire (CfgTier), null si non renseigné.") UUID gestionnaireId,
        @Schema(description = "Nom du gestionnaire.") String gestionnaireNom,
        @Schema(description = "Tiers assigné (CfgTier), null si non renseigné.") UUID assigneId,
        @Schema(description = "Nom de l'assigné.") String assigneNom,
        @Schema(description = "Entité (CfgCompany), null si non renseignée.") UUID entiteId,
        @Schema(description = "Projet (PrjProject), null si non renseigné.") UUID projetId,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Statut ; statut vide en base = Non commencée.") UUID statutId,
        @Schema(description = "Libellé du statut ; statut vide en base = Non commencée.") String statutLibelle) {
}
