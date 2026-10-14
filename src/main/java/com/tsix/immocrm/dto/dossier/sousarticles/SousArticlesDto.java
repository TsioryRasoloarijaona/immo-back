package com.tsix.immocrm.dto.dossier.sousarticles;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Onglet Sous-articles : les lignes et les prix du dossier qui en dépendent.")
public record SousArticlesDto(
        List<SousArticleDto> lignes,
        @Schema(description = "Remise du dossier, pourcentage 0–100, recalculée à chaque ajout ou retrait.")
        BigDecimal remisePercent,
        @Schema(description = "Montant de remise HT, inchangé par un ajout ou un retrait.")
        BigDecimal remiseMontantHT,
        BigDecimal prixAvantRemiseHT,
        BigDecimal prixAvantRemiseTTC,
        BigDecimal prixApresRemiseHT,
        BigDecimal prixApresRemiseTTC
) {
}
