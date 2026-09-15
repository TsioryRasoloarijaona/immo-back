package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Bloc « encaissements » de {@link SyntheseAnnuelleDto} — la seule des trois séries qui porte de
 * l'argent réellement entré.
 */
@Schema(description = "Les encaissements de l'année. Seuls les crédits comptent : un décaissement "
        + "n'est pas un encaissement négatif, il est hors sujet.")
public record EncaissementsAnnuelsDto(

        @Schema(description = "Nombre de lignes de règlement agrégées.", example = "82")
        long nombreLignes,

        @Schema(description = "Somme encaissée sur l'année. `0` si rien n'est entré.",
                example = "7623785.00")
        BigDecimal montantTotal
) {
}
