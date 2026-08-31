package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Body de {@code POST /api/prospections/{id}/devis}. Deux façons de fixer le prix:
 * <ul>
 *   <li>{@code articleId} renseigné (cas normal): les montants HT/TTC/TVA sont calculés à partir
 *       du prix commercial de l'article ({@code StkItem.ComSaleUnitCost(HT)}/{@code
 *       ComSalePrice(HT)}/{@code Vat}) et, s'ils sont fournis, des {@code sousArticleIds}.</li>
 *   <li>{@code articleId} absent: offre "libre", {@code prix} devient directement le prix de
 *       vente TTC (pas de ventilation HT/m²/sous-articles possible sans article).</li>
 * </ul>
 * Si {@code prix} est fourni en plus de {@code articleId}, il remplace le prix commercial de
 * l'article comme prix de vente TTC de départ (avant remise).
 */
public record CreerDevisRequest(

        @Schema(description = "Tranche de l'offre. Obligatoire.")
        UUID trancheId,

        @Schema(description = "Article principal de l'offre. Si renseigné, sert de base au calcul "
                + "HT/TTC/TVA/prix au m². Optionnel (offre libre sans ventilation sinon).")
        UUID articleId,

        @Schema(description = "Sous-articles inclus dans l'offre (leur prix commercial est sommé "
                + "dans \"prix sous-articles\"). Ignoré si articleId est absent.")
        List<UUID> sousArticleIds,

        @Schema(description = "Prospect concerné par l'offre. Si absent, repris du prospect de la "
                + "prospection.")
        UUID prospectId,

        @Schema(description = "Commercial de l'offre. Si absent, repris du commercial de la "
                + "prospection.")
        UUID commercialId,

        @Schema(description = "Mode de paiement envisagé. Optionnel.")
        UUID modePaiementId,

        @Schema(description = "Prix de vente TTC. Obligatoire si articleId est absent (offre "
                + "libre); sinon optionnel et remplace le prix commercial de l'article s'il est "
                + "fourni.")
        BigDecimal prix,

        @Schema(description = "Taux de remise à appliquer, en %. Défaut 0.")
        BigDecimal remisePercent,

        String notes
) {
}
