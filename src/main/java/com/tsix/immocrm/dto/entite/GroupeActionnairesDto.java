package com.tsix.immocrm.dto.entite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Groupe d'actionnaires (CfgGroupShareholder) et ses lignes, par code d'actionnaire.")
public record GroupeActionnairesDto(
        UUID id,
        @Schema(description = "Code, unique.") String code,
        @Schema(description = "Libellé.") String description,
        @Schema(description = "Lignes ; la somme des pourcentages vaut 1.") List<LigneGroupeDto> lignes,
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
