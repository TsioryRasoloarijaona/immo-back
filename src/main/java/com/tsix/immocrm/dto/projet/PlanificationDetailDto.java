package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.domain.planification.NaturePlanification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Planification Gantt complète : en-tête, tâches dans l'ordre de l'arbre, intervenants et versions.")
public record PlanificationDetailDto(
        @Schema(description = "Id de la planification (PKey).") UUID id,
        @Schema(description = "Code unique.") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Date de début (StartDate).") LocalDate dateDebut,
        @Schema(description = "Date et heure de la version ; null hors version.") LocalDateTime dateVersion,
        @Schema(description = "Nature : une VERSION est en lecture seule (toute écriture répond 409).") NaturePlanification nature,
        @Schema(description = "Projet (PrjProject.PKey) ; pour une version, celui de sa planification principale ; null pour un modèle.")
        UUID projetId,
        @Schema(description = "Planification principale d'une version ; null sinon.") UUID principaleId,
        @Schema(description = "Tâches triées par rang (ordre de l'arbre en profondeur).") List<TachePlanificationDto> taches,
        @Schema(description = "Intervenants de la planification, par code.") List<IntervenantPlanificationDto> intervenants,
        @Schema(description = "Versions sauvegardées, la plus récente d'abord ; vide pour une version.")
        List<PlanificationListItemDto> versions) {
}
