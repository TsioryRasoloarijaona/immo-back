package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body de {@code POST /api/prospections/{id}/offre-prix}.
 *
 * <p>Tous les champs sont facultatifs : ne rien fournir signifie « plein tarif du lot », et
 * l'offre est alors calculée à partir du prix commercial de l'article. {@code remisePercent} et
 * {@code prix} sont les deux façons de répondre à « une remise ou un prix final a-t-il été
 * consenti ? » ; fournir les deux est accepté et conserve la sémantique de
 * {@link CreerDevisRequest} — le prix devient la base, la remise s'y applique.
 *
 * <p>Le lot n'est pas dans ce corps : il est pris sur la prospection. C'est ce qui permet au
 * document envoyé d'être exactement celui que le commercial voit sur la fiche, et pourquoi
 * l'endpoint refuse (409) tant qu'aucun lot n'y est rattaché.
 */
@Schema(description = "Conditions de prix à appliquer à l'offre envoyée au prospect. Tout est "
        + "facultatif ; un corps vide signifie « plein tarif du lot ».")
public record EnvoyerOffrePrixRequest(

        @Schema(description = "Taux de remise à appliquer, en %. Défaut 0. Une remise est le cas "
                + "courant : 195 des 298 offres réelles en portent une.", example = "5")
        BigDecimal remisePercent,

        @Schema(description = "Prix de vente TTC imposé, qui remplace le prix commercial du lot "
                + "comme base de calcul. Laisser vide pour partir du prix catalogue.",
                example = "1350000")
        BigDecimal prix,

        @Schema(description = "Mode de paiement envisagé, repris sur l'offre. Optionnel.")
        UUID modePaiementId,

        @Schema(description = "Note libre reprise sur l'offre. Optionnel.")
        String notes
) {
}
