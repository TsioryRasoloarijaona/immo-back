package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Nouvelle échéance (`ComDocument`) ; complétée comme OMRAN (société, module, TVA, HT/TTC).")
public record CreerEcheanceRequest(
        @Schema(description = "Date de l'échéance, enregistrée à minuit. Absente : aujourd'hui.")
        LocalDateTime dateEcheance,
        @Schema(description = "Montant TTC de l'échéance.", example = "100000.00")
        BigDecimal montant,
        @Schema(description = "Libellé. Absent : la description de la catégorie.")
        String description,
        @Schema(description = "Catégorie (`ComSettlementCategory.PKey`). **Requise** (400 sinon).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID categorieId,
        @Schema(description = "Exclue du prix contractuel. Absent : la valeur de la catégorie.")
        Boolean exclureDuPrix,
        @Schema(description = "Commentaire libre.")
        String notes
) {
}
