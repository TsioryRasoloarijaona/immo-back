package com.tsix.immocrm.dto.entite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne d'un groupe d'actionnaires (CfgGroupShareholderLine). En écriture, code et libellé sont ignorés.")
public record LigneGroupeDto(
        @Schema(description = "Actionnaire. Obligatoire en écriture ; inconnu → 400.") UUID actionnaireId,
        @Schema(description = "Code de l'actionnaire (lecture seule).") String actionnaireCode,
        @Schema(description = "Libellé de l'actionnaire (lecture seule).") String actionnaireLibelle,
        @Schema(description = "Pourcentage en fraction (0,25 = 25 %), entre 0 et 1.") BigDecimal pourcentage
) {
}
