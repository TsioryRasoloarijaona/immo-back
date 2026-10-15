package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Modification partielle d'une offre : seuls les champs renseignés changent ; null = inchangé.")
public record ModifierDevisRequest(

        @Schema(description = "Nouvelle tranche. L'article de l'offre doit en faire partie.")
        UUID trancheId,

        UUID articleId,

        List<UUID> sousArticleIds,

        BigDecimal prix,

        @Schema(description = "Taux de remise, en %.")
        BigDecimal remisePercent,

        @Schema(description = "Mode de paiement envisagé.")
        UUID modePaiementId,

        @Schema(description = "Notes. Une chaîne vide efface les notes.")
        String notes
) {
}
