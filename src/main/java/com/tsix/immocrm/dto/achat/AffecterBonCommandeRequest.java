package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Bon de commande auquel rattacher une facture.")
public record AffecterBonCommandeRequest(
        @Schema(description = "Id du bon de commande visible (même fournisseur, société et lot) ; obligatoire.")
        UUID bonCommandeId) {
}
