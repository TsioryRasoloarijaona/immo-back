package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Point de prévision envoyé : date et avancement cumulé.")
public record PointSaisieDto(
        @Schema(description = "Date du point, entre le début et la fin de sa liste.") LocalDate date,
        @Schema(description = "Avancement cumulé (fraction 0–1), croissant avec la date.") BigDecimal avancement
) {
}
