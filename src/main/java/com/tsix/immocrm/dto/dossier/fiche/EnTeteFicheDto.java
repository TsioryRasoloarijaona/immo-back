package com.tsix.immocrm.dto.dossier.fiche;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "En-tête de la fiche dossier : prix de l'article et des sous-articles séparés, HT et TTC.")
public record EnTeteFicheDto(
        @Schema(description = "Superficie (m²) de l'article, figée sur le dossier (ComFolder.Area).")
        BigDecimal superficie,
        @Schema(description = "Prix du m² HT avant remise (ComFolder.UnitCostHT).")
        BigDecimal prixM2HT,
        @Schema(description = "Prix du m² TTC avant remise (ComFolder.UnitCost).")
        BigDecimal prixM2TTC,
        @Schema(description = "Prix article HT avant remise = superficie × prix du m² HT (ComFolder.ItemPriceHT).")
        BigDecimal prixArticleHT,
        @Schema(description = "Prix article TTC avant remise (ComFolder.ItemPrice).")
        BigDecimal prixArticleTTC,
        @Schema(description = "Prix des sous-articles HT avant remise (ComFolder.UnderItemsPriceHT).")
        BigDecimal prixSousArticlesHT,
        @Schema(description = "Prix des sous-articles TTC avant remise (ComFolder.UnderItemsPrice).")
        BigDecimal prixSousArticlesTTC,
        @Schema(description = "Total HT avant remise = prix article HT + prix sous-articles HT.")
        BigDecimal totalHT,
        @Schema(description = "Total TTC avant remise = prix article TTC + prix sous-articles TTC.")
        BigDecimal totalTTC,
        @Schema(description = "Montant de la remise, HT (ComFolder.DiscountAmount).")
        BigDecimal remiseMontantHT,
        @Schema(description = "Remise en pourcentage 0–100 : « Montant remise / Total HT » (aide OMRAN).")
        BigDecimal remisePourcentage,
        @Schema(description = "Prix de vente HT : « Prix article HT après remise + Prix sous article HT après remise ».")
        BigDecimal prixVenteHT,
        @Schema(description = "Prix de vente TTC après remise (ComFolder.PriceAfterDiscount).")
        BigDecimal prixVenteTTC,
        @Schema(description = "Prix contractuel (ComFolder.AgreementPrice) : Σ des lignes non exclues du prix contractuel.")
        BigDecimal prixContractuel
) {
}
