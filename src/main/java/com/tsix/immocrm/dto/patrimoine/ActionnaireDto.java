package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Actionnaire (CfgShareholder), réparti entre entités juridiques et groupes d'actionnaires.")
public record ActionnaireDto(
        @Schema(description = "Identifiant (CfgShareholder.PKey).")
        UUID id,
        @Schema(description = "Code, unique sur toute la table.")
        String code,
        @Schema(description = "Libellé (nom de l'actionnaire).")
        String description,
        @Schema(description = "Notes libres (500 caractères au plus).")
        String notes,
        @Schema(description = "Auteur de la création (libellé du compte).")
        String creePar,
        @Schema(description = "Date de création.")
        LocalDateTime creeLe,
        @Schema(description = "Auteur de la dernière modification.")
        String modifiePar,
        @Schema(description = "Date de la dernière modification.")
        LocalDateTime modifieLe
) {
}
