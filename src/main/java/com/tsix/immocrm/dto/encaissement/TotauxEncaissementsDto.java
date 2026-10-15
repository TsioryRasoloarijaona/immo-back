package com.tsix.immocrm.dto.encaissement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Bande « Total : » de la liste plate des encaissements, sur tout le filtre (toutes pages).")
public record TotauxEncaissementsDto(
        @Schema(description = "Nombre de règlements du filtre, annulés compris (comme totalElements de la liste).")
        long nombre,
        @Schema(description = "Σ montantBrut, règlements CANCELED exclus.")
        BigDecimal montantBrut,
        @Schema(description = "Σ montantBrutRestant (reste à affecter), règlements CANCELED exclus.")
        BigDecimal montantBrutRestant,
        @Schema(description = "Σ montantNet, règlements CANCELED exclus.")
        BigDecimal montantNet,
        @Schema(description = "Σ montantBase, règlements CANCELED exclus.")
        BigDecimal montantBase
) {
}
