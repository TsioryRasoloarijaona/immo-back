package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Prix saisi sur une ligne d'offre.")
public record PrixLigneOffre(
        @Schema(description = "PKey de la ligne d'offre (BuyDocumentLine) de ce devis.") UUID ligneId,
        @Schema(description = "Prix unitaire HT, positif ou nul.") BigDecimal prixUnitaireHT,
        @Schema(description = "Remise en fraction entre 0 et 1 ; null = 0.") BigDecimal remise) {
}
