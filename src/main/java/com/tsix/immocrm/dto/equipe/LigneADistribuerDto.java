package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Une pré-prospection ouverte et sans commercial de la carte « À distribuer ».")
public record LigneADistribuerDto(

        @Schema(description = "Identifiant de la pré-prospection (`ComProspection.PKey`).")
        UUID id,

        @Schema(description = "Code de la pré-prospection.", example = "PP-2026-00412")
        String code,

        @Schema(description = "Nom du prospect lié, sinon `TempDescription` saisi à la création. `null` si aucun.")
        String nom,

        @Schema(description = "Code de la tranche. `null` = sans tranche.", example = "BEL-AIR-T1")
        String trancheCode,

        @Schema(description = "Projet de la tranche. `null` = sans tranche ou tranche sans projet.")
        UUID projetId,

        @Schema(description = "Date de la pré-prospection (`DocDate`).")
        LocalDateTime dateDocument,

        @Schema(description = "Vrai si au moins une action non système existe ; toujours faux quand `toutes=false`.")
        boolean dejaTraitee
) {
}
