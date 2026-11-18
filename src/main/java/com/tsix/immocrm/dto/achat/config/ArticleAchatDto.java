package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Article d'achat (BuyItem) : ligne de la liste et fiche, mêmes champs. Ratios tels que stockés (0,2 = 20 %).")
public record ArticleAchatDto(
        @Schema(description = "BuyItem.PKey.") UUID id,
        @Schema(description = "Code, unique parmi les articles.") String code,
        String description,
        @Schema(description = "Catégorie de trésorerie (StlCategory.PKey).") UUID categorieComptableId,
        @Schema(description = "Code de la catégorie.") String categorieComptableCode,
        @Schema(description = "Famille, dérivée de la sous-famille (lecture seule).") UUID familleId,
        @Schema(description = "Code de la famille dérivée.") String familleCode,
        @Schema(description = "Sous-famille (BuyItemUnderFamily.PKey).") UUID sousFamilleId,
        @Schema(description = "Code de la sous-famille.") String sousFamilleCode,
        @Schema(description = "Unité (texte libre).") String unite,
        @Schema(description = "P.U. HT ; 0 si non saisi.") BigDecimal prixUnitaireHT,
        @Schema(description = "Ratio FODEC.") BigDecimal fodec,
        @Schema(description = "Coût logistique HT.") BigDecimal coutLogistiqueHT,
        @Schema(description = "Ratio de TVA du coût logistique.") BigDecimal coutLogistiqueTva,
        @Schema(description = "Coût indirect HT.") BigDecimal coutIndirectHT,
        @Schema(description = "Ratio de TVA du coût indirect.") BigDecimal coutIndirectTva,
        @Schema(description = "Coût financier HT (colonne CostFinacialHT).") BigDecimal coutFinancierHT,
        @Schema(description = "Ratio de TVA du coût financier.") BigDecimal coutFinancierTva,
        @Schema(description = "Vente P.U. HT.") BigDecimal prixVenteHT,
        @Schema(description = "Ratio de TVA de la vente.") BigDecimal venteTva,
        @Schema(description = "Notes, 500 caractères au plus.") String notes
) {
}
