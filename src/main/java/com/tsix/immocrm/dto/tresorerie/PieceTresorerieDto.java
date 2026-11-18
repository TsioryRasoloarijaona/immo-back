package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Pièce réglée : facture d'achat (BuyDocument) ou pièce de recette (StlDocument).")
public record PieceTresorerieDto(
        @Schema(description = "PKey de la pièce.") UUID id,
        @Schema(description = "DocType de la pièce.", example = "INVOICE") String type,
        @Schema(description = "Code de la pièce.") String libelle) {
}
