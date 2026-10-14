package com.tsix.immocrm.dto.dossier.modalite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de modalité de paiement saisie à la main.")
public record CreerModalitePaiementRequest(
        @Schema(description = "Date (enregistrée à minuit). Requise.", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime date,
        @Schema(description = "Catégorie (`ComSettlementCategory.PKey`). Requise.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID categorieId,
        @Schema(description = "Montant TTC. Requis.", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal montant,
        String notes
) {
}
