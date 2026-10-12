package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Groupe d'actionnaires (CfgGroupShareholder) : modèle de répartition en pourcentages.")
public record GroupeActionnairesDto(
        @Schema(description = "Identifiant (CfgGroupShareholder.PKey).")
        UUID id,
        @Schema(description = "Code, unique sur toute la table.")
        String code,
        @Schema(description = "Libellé.")
        String description,
        @Schema(description = "Lignes du groupe, triées par code d'actionnaire ; leurs pourcentages font 100 %.")
        List<Ligne> lignes,
        @Schema(description = "Auteur de la création.")
        String creePar,
        @Schema(description = "Date de création.")
        LocalDateTime creeLe,
        @Schema(description = "Auteur de la dernière modification.")
        String modifiePar,
        @Schema(description = "Date de la dernière modification.")
        LocalDateTime modifieLe
) {

    @Schema(name = "LigneGroupeActionnaires", description = "Part d'un actionnaire dans le groupe.")
    public record Ligne(
            @Schema(description = "Identifiant de la ligne (CfgGroupShareholderLine.PKey).")
            UUID id,
            @Schema(description = "Actionnaire (CfgShareholder.PKey).")
            UUID actionnaireId,
            @Schema(description = "Code de l'actionnaire.")
            String actionnaireCode,
            @Schema(description = "Libellé de l'actionnaire.")
            String actionnaireLibelle,
            @Schema(description = "Pourcentage en fraction : 0.4 = 40 %.", example = "0.4")
            BigDecimal pourcentage
    ) {
    }
}
