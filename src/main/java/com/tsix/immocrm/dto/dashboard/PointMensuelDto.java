package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Un mois de l'échéancier. Montant *dû* sur le mois, pas encaissé — des mois "
        + "futurs peuvent donc être renseignés.")
public record PointMensuelDto(

        @Schema(description = "Numéro du mois, de 1 à 12.", example = "8")
        int mois,

        @Schema(description = "Somme des échéances du mois. `0` quand il n'y en a aucune, jamais "
                + "`null` — un mois vide reste un point de la courbe.", example = "5766900.00")
        BigDecimal montant,

        @Schema(description = "Nombre d'échéances agrégées. Utile pour distinguer « un mois vide » "
                + "de « un mois à zéro ».", example = "8")
        long lignes
) {
}
