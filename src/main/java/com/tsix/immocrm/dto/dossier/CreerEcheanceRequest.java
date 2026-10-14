package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Nouvelle ligne d'échéancier ; société, TVA, HT et module sont dérivés de la catégorie et du dossier.")
public record CreerEcheanceRequest(
        @Schema(description = "Date d'exigibilité (`ComDocument.DocDate`), enregistrée à minuit.")
        LocalDateTime dateEcheance,

        @Schema(description = "Montant TTC (`ComDocument.Amount`).", example = "100000.00")
        BigDecimal montant,

        @Schema(description = "Libellé ; absent, celui de la catégorie.")
        String description,

        @Schema(description = "Catégorie (`ComSettlementCategory.PKey`). Requise : 400 si absente.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID categorieId,

        @Schema(description = "Exclue du prix contractuel ; absent, la valeur de la catégorie.")
        Boolean exclureDuPrix,

        @Schema(description = "Commentaire libre.")
        String notes
) {
}
