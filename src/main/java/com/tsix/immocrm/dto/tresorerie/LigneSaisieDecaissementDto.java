package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Une affectation saisie : pièce d'achat réglée et montant affecté.")
public record LigneSaisieDecaissementDto(
        @Schema(description = "Pièce affectée (BuyDocument.PKey), visible, du même fournisseur, non soldée.")
        UUID pieceId,
        @Schema(description = "Montant affecté, > 0 et ≤ reste à payer de la pièce.", example = "300")
        BigDecimal montant) {
}
