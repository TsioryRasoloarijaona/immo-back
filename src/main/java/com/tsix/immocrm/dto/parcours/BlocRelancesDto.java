package com.tsix.immocrm.dto.parcours;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Bloc de relances : total et les 20 premières lignes.")
public record BlocRelancesDto(
        @Schema(description = "Nombre total de lignes du bloc, au-delà des 20 servies.", example = "3")
        long total,
        @Schema(description = "Les 20 premières lignes, par échéance croissante.")
        List<RelanceJourneeDto> lignes
) {
}
