package com.tsix.immocrm.dto.dossier.sousarticles;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Article libre de la tranche et de la vocation du dossier, ajoutable comme sous-article.")
public record SousArticleDisponibleDto(
        @Schema(description = "Article du stock (`StkItem.PKey`), à passer à l'ajout.")
        UUID id,
        String code,
        String description,
        String categorie,
        String type,
        BigDecimal superficie,
        @Schema(description = "Prix de vente HT du stock (`StkItem.SalePriceHT`).")
        BigDecimal prixHT,
        @Schema(description = "Taux de TVA, ratio (0.2 = 20 %).")
        BigDecimal tauxTVA
) {
}
