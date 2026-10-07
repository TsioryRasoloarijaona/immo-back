package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record EncaissementsAnnuelsDto(

        @Schema(description = "Nombre de lignes de règlement agrégées.", example = "82")
        long nombreLignes,

        @Schema(description = "Somme encaissée sur l'année. `0` si rien n'est entré.",
                example = "7623785.00")
        BigDecimal montantTotal
) {
}
