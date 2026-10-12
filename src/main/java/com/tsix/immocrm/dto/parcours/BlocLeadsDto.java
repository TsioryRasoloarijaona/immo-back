package com.tsix.immocrm.dto.parcours;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Bloc de nouveaux leads : total et les 20 premières lignes.")
public record BlocLeadsDto(
        @Schema(description = "Nombre total de lignes du bloc, au-delà des 20 servies.", example = "12")
        long total,
        @Schema(description = "Les 20 premières lignes, les plus récentes d'abord.")
        List<LeadJourneeDto> lignes
) {
}
