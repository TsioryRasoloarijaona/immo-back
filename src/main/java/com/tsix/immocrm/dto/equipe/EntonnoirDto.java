package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EntonnoirDto(

        @Schema(description = "Premier jour de la cohorte, inclus.", example = "2026-09-12")
        LocalDate du,

        @Schema(description = "Dernier jour de la cohorte, inclus jusqu'à 23:59:59.", example = "2026-10-11")
        LocalDate au,

        @Schema(description = "Projet filtré, `null` = tout le périmètre de l'appelant.")
        UUID projetId,

        @Schema(description = "Les six étapes dans l'ordre, chacune ≤ la précédente.")
        List<EtapeEntonnoirDto> etapes,

        Long delaiMedianJours
) {
}
