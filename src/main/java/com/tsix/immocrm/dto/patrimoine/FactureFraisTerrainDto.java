package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Facture fournisseur (BuyDocument LAND_EXPENSE) créée par « Valider pour règlement ».")
public record FactureFraisTerrainDto(
        @Schema(description = "BuyDocument.PKey.") UUID factureId,
        @Schema(description = "Code de la séquence « Facture fournisseur », ex. 27/03014.") String code,
        @Schema(description = "Montant TTC = montant du frais, sans arrondi.") BigDecimal montant
) {
}
