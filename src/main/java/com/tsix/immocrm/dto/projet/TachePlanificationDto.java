package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.domain.planification.ContrainteTache;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Tâche d'une planification (CfgGanttTask).")
public record TachePlanificationDto(
        @Schema(description = "Id de la tâche (PKey).") UUID id,
        @Schema(description = "Tâche parente ; null pour une racine.") UUID parentId,
        @Schema(description = "Code (« 1 », « 1.2 »…).") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Rang de ligne, à partir de 1, recalculé par le serveur (Sorting).") Integer rang,
        @Schema(description = "Niveau dans l'arbre, 0 pour une racine (Depth).") Integer profondeur,
        @Schema(description = "Date de début.") LocalDate dateDebut,
        @Schema(description = "Durée en jours entiers.") Integer duree,
        @Schema(description = "Fin calculée = début + durée (jamais stockée).") LocalDate dateFin,
        @Schema(description = "Avancement en pourcentage (0 à 100), stocké tel quel.") BigDecimal avancement,
        @Schema(description = "Alerte, en jours avant le début.") Integer alerteAvantJours,
        @Schema(description = "Contrainte de planification.") ContrainteTache contrainte,
        @Schema(description = "Date de la contrainte.") LocalDate dateContrainte,
        @Schema(description = "Échéance (Deadline).") LocalDate echeance,
        @Schema(description = "Jalon (losange dans le diagramme).") Boolean jalon,
        @Schema(description = "Prédécesseurs de la tâche.") List<PredecesseurDto> predecesseurs,
        @Schema(description = "Ids des intervenants de la tâche (ResourceNames).") List<UUID> intervenants,
        @Schema(description = "Type de tâche (CfgGanttTaskType.PKey).") UUID typeTacheId,
        @Schema(description = "Lot du projet rattaché au clonage (PrjMarket.PKey) ; null pour un modèle.") UUID lotId,
        @Schema(description = "Catégorie de dépense (StlCategory.PKey), recopiée du modèle.") UUID categorieId) {
}
