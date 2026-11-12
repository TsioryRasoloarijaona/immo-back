package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ventilation de la pièce sur un projet (BuyVentilationExpense) ; tous les projets, même non habilités.")
public record VentilationProjetAchatDto(
        @Schema(description = "Id du projet (PrjProject).") UUID projetId,
        @Schema(description = "Code du projet.") String projetCode,
        @Schema(description = "Libellé du projet.") String projetDescription,
        @Schema(description = "Taux de ventilation, fraction (0,6 = 60 %).", example = "0.6") BigDecimal taux) {
}
