package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Zone enregistrée, valeurs recalculées par le serveur.")
public record ZoneTerrainDto(
        @JsonUnwrapped ZoneTerrainEcritureDto zone,
        @Schema(description = "Lecture seule : vendable − bureau − commerce (HabitatArea).") BigDecimal superficieHabitat
) {
}
