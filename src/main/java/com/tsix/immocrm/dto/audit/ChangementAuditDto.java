package com.tsix.immocrm.dto.audit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Une propriété modifiée, telle qu'écrite dans le JSON Details d'OMRAN.")
public record ChangementAuditDto(
        @Schema(description = "Nom de colonne OMRAN, ou nom de navigation pour une clé étrangère.", example = "ComSalePriceHT")
        String propriete,
        @Schema(description = "Valeur avant ; chaîne vide pour une création.", example = "0")
        String ancienneValeur,
        @Schema(description = "Valeur après, en texte OMRAN (dates jj/MM/aaaa HH:mm:ss, décimaux à point).",
                example = "845000.000000")
        String nouvelleValeur
) {
}
