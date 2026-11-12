package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Récapitulatif des lignes de la pièce pour un taux de TVA (sous-grille Base / TVA / Montant).")
public record RecapTvaAchatDto(
        @Schema(description = "Taux de TVA en % (VatRatio × 100) ; taux absent compté à 0.", example = "20") BigDecimal tauxTVA,
        @Schema(description = "Base : Σ totalHT des lignes de ce taux.") BigDecimal base,
        @Schema(description = "Montant de TVA : Σ totalTVA des lignes de ce taux.") BigDecimal montantTVA) {
}
