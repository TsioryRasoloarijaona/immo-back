package com.tsix.immocrm.dto.dossier.sousarticles;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Sous-article du dossier (`ComFolderItem` à `IsUnderItem`), prix figés pour ce dossier.")
public record SousArticleDto(
        @Schema(description = "Ligne du dossier (`ComFolderItem.PKey`), à passer au retrait.")
        UUID id,
        @Schema(description = "Article du stock (`StkItem.PKey`).")
        UUID articleId,
        String code,
        String description,
        @Schema(description = "Catégorie d'article (code).")
        String categorie,
        @Schema(description = "Type d'article (code).")
        String type,
        @Schema(description = "Superficie en m².")
        BigDecimal superficie,
        @Schema(description = "Taux de TVA, ratio (0.2 = 20 %).")
        BigDecimal tauxTVA,
        BigDecimal prixAvantRemiseHT,
        BigDecimal prixAvantRemiseTTC,
        @Schema(description = "Remise appliquée, pourcentage 0–100 (le taux du dossier).")
        BigDecimal remisePercent,
        BigDecimal prixHT,
        BigDecimal prixTTC,
        @Schema(description = "Affecté par défaut à l'article : ne peut pas être retiré.")
        boolean affecteParDefaut,
        @Schema(description = "Sous-article d'origine de l'article (hors superficie vendue).")
        boolean sousArticleDOrigine
) {
}
