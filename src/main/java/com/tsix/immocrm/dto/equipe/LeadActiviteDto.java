package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "État d'activité d'un lead (pré-prospection ou prospection) de la fenêtre « Leads de … ».")
public record LeadActiviteDto(

        @Schema(description = "Lead (ComProspection.PKey) demandé.")
        UUID id,

        @Schema(description = "Nombre d'actions non système du lead (ComActionMessage, IsSystem absent ou faux).", example = "3")
        int nombreActions,

        @Schema(description = "Date de la plus récente de ces actions ; `null` si aucune.", example = "2026-10-02T14:30:00")
        LocalDateTime derniereActionDate,

        @Schema(description = "Jours entiers depuis la dernière action, à défaut depuis la date du lead.", example = "11")
        int joursSansAction,

        boolean endormi,

        boolean jamaisTraite
) {
}
