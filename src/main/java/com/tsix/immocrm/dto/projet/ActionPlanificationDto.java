package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Corps des actions de planification (clonage, date de début, ordre des tâches). */
public final class ActionPlanificationDto {

    private ActionPlanificationDto() {
    }

    @Schema(description = "Clonage d'un modèle vers un projet.")
    public record ClonagePlanification(
            @Schema(description = "Modèle source (CfgGanttProject.PKey), obligatoire.") UUID modeleId) {
    }

    @Schema(description = "« Modifier la date de début » de la planification.")
    public record DateDebutPlanification(@Schema(description = "Nouvelle date de début, obligatoire.") LocalDate dateDebut) {
    }

    @Schema(description = "Nouvel ordre des sœurs d'un même parent.")
    public record OrdreTachesPlanification(
            @Schema(description = "Ids de toutes les sœurs, dans l'ordre voulu.") List<UUID> tacheIds) {
    }
}
