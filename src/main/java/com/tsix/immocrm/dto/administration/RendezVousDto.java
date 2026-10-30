package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Rendez-vous (ligne AdmTask de l'entité AdmAppointment), sans statut ni workflow.")
public record RendezVousDto(
        @Schema(description = "PKey du rendez-vous.") UUID id,
        @Schema(description = "Code issu de la séquence AdmTask, jamais saisi.", example = "26/00110") String code,
        @Schema(description = "Description, obligatoire.") String description,
        @Schema(description = "Déclencheur (CfgTier), null si non renseigné.") UUID declencheurId,
        @Schema(description = "Code du déclencheur.") String declencheurCode,
        @Schema(description = "Nom du déclencheur.") String declencheurNom,
        @Schema(description = "Date prévisionnelle, clé de tri de la liste.") LocalDateTime datePrevue,
        @Schema(description = "Durée prévisionnelle (entier, unité d'OMRAN).") Integer dureePrevue,
        @Schema(description = "Date de fin prévisionnelle.") LocalDateTime dateFinPrevue,
        @Schema(description = "Personnel (CfgTier), null si non renseigné.") UUID gestionnaireId,
        @Schema(description = "Code du personnel.") String gestionnaireCode,
        @Schema(description = "Nom du personnel.") String gestionnaireNom,
        @Schema(description = "Tier (CfgTier, colonne CfgProspectId), null si non renseigné.") UUID tierId,
        @Schema(description = "Code du tier, null si tier absent.") String tierCode,
        @Schema(description = "Nom du tier, null si tier absent.") String tierNom,
        @Schema(description = "Objet source, écrit par OMRAN, lecture seule.") UUID sourceId,
        @Schema(description = "Libellé de la source, écrit par OMRAN, lecture seule.") String sourceLibelle,
        @Schema(description = "Entité fonctionnelle de la source (Description de StrEntity).") String sourceEntite,
        @Schema(description = "Notes.") String notes) {
}
