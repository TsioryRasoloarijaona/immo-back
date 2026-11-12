package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Soldes fournisseurs comme OMRAN (BuySupplierPositionView) : une ligne par (fournisseur, société) et le total.")
public record PositionsFournisseursDto(
        @Schema(description = "Lignes triées par nom de fournisseur puis code société.")
        List<PositionFournisseurDto> lignes,
        @Schema(description = "Somme des lignes rendues ; fournisseurId, societeId et societeCode nuls.")
        PositionFournisseurDto total) {
}
