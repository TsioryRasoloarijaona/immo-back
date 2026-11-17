package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Échéance de remboursement de RG saisie (PrjMarketWarrantyLine), envoyée avec toute la grille.")
public record EcheanceRgSaisieDto(
        @Schema(description = "Id ; UUID v4 généré par le front pour une ligne neuve.") UUID id,
        @Schema(description = "Ordre (Sorting), obligatoire.") Integer ordre,
        @Schema(description = "Description, 255 caractères au plus.", example = "Échéance 1") String description,
        @Schema(description = "Délai en jours ≥ 0 ; s'il est présent, l'échéance est recalculée.") Integer delaiJours,
        @Schema(description = "Date d'échéance ; sans délai, le délai est calculé depuis la réception définitive.") LocalDate echeance,
        @Schema(description = "Pourcentage 0–100, obligatoire.", example = "50") BigDecimal pourcentage
) {
}
