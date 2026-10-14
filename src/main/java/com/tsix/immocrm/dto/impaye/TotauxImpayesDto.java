package com.tsix.immocrm.dto.impaye;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totaux sur toutes les lignes du filtre, pas seulement sur la page.")
public record TotauxImpayesDto(
        @Schema(description = "Somme des montantAPayer.")
        BigDecimal montantAPayer,
        @Schema(description = "Somme des montantRegle.")
        BigDecimal montantRegle,
        @Schema(description = "Somme des montantReste.")
        BigDecimal montantReste,
        @Schema(description = "Nombre de dossiers distincts parmi les lignes.")
        long nombreDossiers
) {
}
