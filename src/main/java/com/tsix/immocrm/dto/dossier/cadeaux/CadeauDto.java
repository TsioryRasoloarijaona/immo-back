package com.tsix.immocrm.dto.dossier.cadeaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Cadeau du dossier (`ComFolderGift`).")
public record CadeauDto(
        UUID id,
        @Schema(description = "Cadeau du catalogue (`ComGift.PKey`) ; null si pas encore choisi.")
        UUID cadeauId,
        String cadeau,
        BigDecimal prix,
        boolean livre,
        @Schema(description = "En stock au moment du choix du cadeau.")
        boolean disponible,
        UUID raisonId,
        String raison,
        @Schema(description = "Date du bon d'achat.")
        LocalDateTime dateBon,
        LocalDateTime dateLivraison,
        String notes
) {
}
