package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code prix} et {@code remisePercent} ensemble : le prix devient la base, la remise s'y applique
 * (comme {@link CreerDevisRequest}). Le lot est pris sur la prospection, d'où le 409 sans lot.
 */
public record EnvoyerOffrePrixRequest(

        @Schema(description = "Taux de remise à appliquer, en %. Défaut 0. Une remise est le cas "
                + "courant : 195 des 298 offres réelles en portent une.", example = "5")
        BigDecimal remisePercent,

        @Schema(example = "1350000")
        BigDecimal prix,

        @Schema(description = "Mode de paiement envisagé, repris sur l'offre. Optionnel.")
        UUID modePaiementId,

        @Schema(description = "Note libre reprise sur l'offre. Optionnel.")
        String notes
) {
}
