package com.tsix.immocrm.dto.remise;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Demande de remise d'une agence sur un dossier.")
public record DemanderRemiseRequest(
        @Schema(description = "Taux de remise demandé, en pourcentage (> 0 et ≤ 100), supérieur au taux actuel du dossier.",
                example = "5")
        BigDecimal taux,

        @Schema(description = "Justification de la demande, obligatoire, 1 000 caractères au plus.")
        String motif
) {
}
