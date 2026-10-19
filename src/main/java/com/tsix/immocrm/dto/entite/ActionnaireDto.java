package com.tsix.immocrm.dto.entite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Actionnaire (CfgShareholder).")
public record ActionnaireDto(
        UUID id,
        @Schema(description = "Code, unique.") String code,
        @Schema(description = "Libellé.") String description,
        @Schema(description = "Notes (500 caractères au plus).") String notes,
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
