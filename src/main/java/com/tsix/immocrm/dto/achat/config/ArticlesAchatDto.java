package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Écran Articles d'achat : lignes triées par code et actions permises au compte courant.")
public record ArticlesAchatDto(
        List<ArticleAchatDto> articles,
        @Schema(description = "Droit Yes.Buy.Front.Config.BuyItemList.toolNew : créer et modifier.") boolean peutEcrire,
        @Schema(description = "Droit Yes.Buy.Front.Config.BuyItemList.toolDelete : supprimer.") boolean peutSupprimer,
        @Schema(description = "Droit Yes.Buy.Front.Config.BuyItemList.toolExport : exporter la liste.") boolean peutExporter
) {
}
