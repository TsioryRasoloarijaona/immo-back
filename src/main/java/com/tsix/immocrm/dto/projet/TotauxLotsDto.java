package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totaux de la liste des lots : mêmes filtres, toutes pages ; Σ des champs homonymes des lignes.")
public record TotauxLotsDto(
        @Schema(description = "Nombre de lots filtrés (= totalElements de la liste).") long nombreLots,
        @Schema(description = "Σ coutPrevisionnelHT.") BigDecimal coutPrevisionnelHT,
        @Schema(description = "Σ coutPrevisionnelTTC.") BigDecimal coutPrevisionnelTTC,
        @Schema(description = "Σ coutPrevisionnelNetAPayer.") BigDecimal coutPrevisionnelNetAPayer,
        @Schema(description = "Σ coutPrevisionnelRegle.") BigDecimal coutPrevisionnelRegle,
        @Schema(description = "Σ coutPrevisionnelReste.") BigDecimal coutPrevisionnelReste
) {
}
