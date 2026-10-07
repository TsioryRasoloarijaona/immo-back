package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Sans {@code articleId}, {@code prix} est directement le prix de vente TTC (offre libre) ; avec,
 * il remplace le prix commercial de l'article comme base avant remise.
 */
public record CreerDevisRequest(

        @Schema(description = "Tranche de l'offre. Obligatoire.")
        UUID trancheId,

        UUID articleId,

        List<UUID> sousArticleIds,

        @Schema(description = "Prospect concerné par l'offre. Si absent, repris du prospect de la "
                + "prospection.")
        UUID prospectId,

        @Schema(description = "Commercial de l'offre. Si absent, repris du commercial de la "
                + "prospection.")
        UUID commercialId,

        @Schema(description = "Mode de paiement envisagé. Optionnel.")
        UUID modePaiementId,

        BigDecimal prix,

        @Schema(description = "Taux de remise à appliquer, en %. Défaut 0.")
        BigDecimal remisePercent,

        String notes
) {
}
