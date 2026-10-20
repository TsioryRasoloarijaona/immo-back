package com.tsix.immocrm.dto.desistement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Page de désistements et totaux du filtre complet.")
public record DesistementsPageDto(
        @Schema(description = "Lignes de la page demandée.")
        List<DesistementDto> lignes,
        @Schema(description = "Numéro de la page (à partir de 0).")
        int page,
        @Schema(description = "Taille de page demandée.")
        int size,
        @Schema(description = "Nombre total de lignes du filtre.")
        long total,
        @Schema(description = "Nombre total de pages.")
        int totalPages,
        @Schema(description = "Totaux sur toutes les lignes du filtre.")
        TotauxDesistementsDto totaux
) {

    @Schema(description = "Totaux d'une liste de désistements.")
    public record TotauxDesistementsDto(
            @Schema(description = "Σ total réglé.")
            BigDecimal totalRegle,
            @Schema(description = "Σ pénalité après remise.")
            BigDecimal penaliteApresRemise,
            @Schema(description = "Σ reste frais à régler.")
            BigDecimal resteFraisARegler,
            @Schema(description = "Σ à rembourser.")
            BigDecimal aRembourser
    ) {
    }
}
