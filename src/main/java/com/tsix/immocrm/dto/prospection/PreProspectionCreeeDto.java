package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Un doublon n'empêche pas la création : voir {@code FEATURE-creation-pre-prospection.md} §3. */
public record PreProspectionCreeeDto(
        @Schema(description = "La pré-prospection créée, sous la même forme que "
                + "`GET /api/prospections/{id}`.")
        ProspectionDetailDto preProspection,
        List<DoublonPreProspectionDto> doublons
) {
}
