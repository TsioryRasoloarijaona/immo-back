package com.tsix.immocrm.dto.dossier.fiche;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Onglet « Autres montants » de la fiche OMRAN : prix avant et après remise, HT et TTC.")
public record AutresMontantsFicheDto(
        @Schema(description = "« Sup (m²) » : superficie en m² (ComFolder.Area).")
        BigDecimal superficie,
        @Schema(description = "Superficie globale = article + sous-articles (ComFolder.GlobalArea).")
        BigDecimal superficieGlobale,
        @Schema(description = "Taux de TVA en ratio 0–1 (ComFolder.VATRatio) ; 0,2 = 20 %.")
        BigDecimal tauxTva,
        @Schema(description = "« Prix article m² HT » : prix d'un m² HT repris du module stock (ComFolder.UnitCostHT).")
        BigDecimal prixArticleM2HT,
        @Schema(description = "« Prix article m² TTC » : prix du m² HT × (1 + TVA).")
        BigDecimal prixArticleM2TTC,
        @Schema(description = "Prix article HT avant remise.")
        BigDecimal prixArticleHT,
        @Schema(description = "Prix article TTC avant remise.")
        BigDecimal prixArticleTTC,
        @Schema(description = "Prix des sous-articles HT avant remise.")
        BigDecimal prixSousArticlesHT,
        @Schema(description = "Prix des sous-articles TTC avant remise.")
        BigDecimal prixSousArticlesTTC,
        @Schema(description = "Prix article HT après remise (ComFolder.ItemPriceAfterHT).")
        BigDecimal prixArticleApresRemiseHT,
        @Schema(description = "Prix article TTC après remise (ComFolder.ItemPriceAfter).")
        BigDecimal prixArticleApresRemiseTTC,
        @Schema(description = "Prix des sous-articles HT après remise (ComFolder.UnderItemsPriceAfterHT).")
        BigDecimal prixSousArticlesApresRemiseHT,
        @Schema(description = "Prix des sous-articles TTC après remise (ComFolder.UnderItemsPriceAfter).")
        BigDecimal prixSousArticlesApresRemiseTTC,
        @Schema(description = "Total HT avant remise.")
        BigDecimal totalHT,
        @Schema(description = "Total TTC avant remise.")
        BigDecimal totalTTC,
        @Schema(description = "Remise en pourcentage 0–100 (Montant remise / Total HT).")
        BigDecimal remisePourcentage,
        @Schema(description = "Prix de vente HT après remise (ComFolder.PriceAfterDiscountHT).")
        BigDecimal prixVenteHT,
        @Schema(description = "Prix de vente TTC après remise (ComFolder.PriceAfterDiscount).")
        BigDecimal prixVenteTTC,
        @Schema(description = "Prix de vente au m² HT = prix de vente HT / superficie vendue ; null si superficie nulle.")
        BigDecimal prixVenteM2HT,
        @Schema(description = "Prix de vente au m² TTC = prix de vente TTC / superficie vendue ; null si superficie nulle.")
        BigDecimal prixVenteM2TTC
) {
}
