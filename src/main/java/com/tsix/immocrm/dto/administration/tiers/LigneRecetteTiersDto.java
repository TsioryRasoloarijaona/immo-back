package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Pièce de recette du tiers (StlDocument.CfgTierId), montants tels qu'en base.")
public record LigneRecetteTiersDto(
        @Schema(description = "PKey de la pièce.") UUID id,
        @Schema(description = "Code.") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Date de la pièce (DocDate), clé du tri.") LocalDateTime date,
        @Schema(description = "Échéance (MaturityDate).") LocalDateTime echeance,
        @Schema(description = "Référence (DocRef).") String reference,
        @Schema(description = "Montant à payer (AmountToPay).") BigDecimal aPayer,
        @Schema(description = "Notes.") String notes) implements LigneOngletTiers {
}
