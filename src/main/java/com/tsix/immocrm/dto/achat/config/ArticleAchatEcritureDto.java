package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Saisie d'un article d'achat. Création : fiche complète ; modification : champ absent ou null = inchangé.")
public record ArticleAchatEcritureDto(
        @Schema(description = "Obligatoire, unique parmi les articles (409 sinon).") String code,
        @Schema(description = "Obligatoire.") String description,
        @Schema(description = "Obligatoire : StlCategory.PKey.") UUID categorieComptableId,
        @Schema(description = "Facultatif : contrôle seulement, 400 s'il n'est pas la famille de la sous-famille.")
        UUID familleId,
        @Schema(description = "Obligatoire : BuyItemUnderFamily.PKey ; donne la famille de l'article.") UUID sousFamilleId,
        String unite,
        @Schema(description = "Absent à la création = 0.") BigDecimal prixUnitaireHT,
        @Schema(description = "Ratio ; absent à la création = 0.") BigDecimal fodec,
        BigDecimal coutLogistiqueHT,
        @Schema(description = "Ratio.") BigDecimal coutLogistiqueTva,
        BigDecimal coutIndirectHT,
        @Schema(description = "Ratio.") BigDecimal coutIndirectTva,
        BigDecimal coutFinancierHT,
        @Schema(description = "Ratio.") BigDecimal coutFinancierTva,
        BigDecimal prixVenteHT,
        @Schema(description = "Ratio.") BigDecimal venteTva,
        @Schema(description = "500 caractères au plus.") String notes
) {
}
