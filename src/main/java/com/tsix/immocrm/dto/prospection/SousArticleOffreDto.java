package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Sous-article d'une offre (StlQuoteItem), avec ses prix figés dans l'offre.")
public record SousArticleOffreDto(
        @Schema(description = "Id de la ligne StlQuoteItem.")
        UUID id,
        @Schema(description = "Id de l'article (StkItem) du sous-article.")
        UUID articleId,
        @Schema(description = "Code du sous-article.")
        String code,
        @Schema(description = "Désignation du sous-article.")
        String description,
        @Schema(description = "Nature du bien (StkItemCategory), ex. \"JARDIN\".")
        String categorie,
        @Schema(description = "Superficie, en m².")
        BigDecimal superficie,
        @Schema(description = "Prix TTC avant remise.")
        BigDecimal prixAvantRemiseTTC,
        @Schema(description = "Montant TTC de la remise.")
        BigDecimal remiseTTC,
        @Schema(description = "Prix HT après remise.")
        BigDecimal prixApresRemiseHT,
        @Schema(description = "Prix TTC après remise.")
        BigDecimal prixApresRemiseTTC
) {
}
