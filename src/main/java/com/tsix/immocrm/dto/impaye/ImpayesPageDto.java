package com.tsix.immocrm.dto.impaye;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Diffère de {@code PageDto} : les totaux portent sur tout le filtre ({@code FEATURE-impayes.md} R7). */
@Schema(description = "Page d'impayés et totaux du filtre complet.")
public record ImpayesPageDto(
        @Schema(description = "Lignes de la page demandée.")
        List<ImpayeDto> lignes,
        @Schema(description = "Numéro de la page (à partir de 0).")
        int page,
        @Schema(description = "Taille de page demandée.")
        int size,
        @Schema(description = "Nombre total de lignes du filtre.")
        long total,
        @Schema(description = "Nombre total de pages.")
        int totalPages,
        @Schema(description = "Totaux sur toutes les lignes du filtre.")
        TotauxImpayesDto totaux
) {
}
