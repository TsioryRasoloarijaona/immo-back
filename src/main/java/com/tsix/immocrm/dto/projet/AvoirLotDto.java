package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Avoir du lot (BuyDocument DocType REFUND).")
public record AvoirLotDto(
        @Schema(description = "BuyDocument.PKey.") UUID id,
        @Schema(description = "Date de l'avoir (DocDate).") LocalDate date,
        @Schema(description = "Code de l'avoir.") String code,
        @Schema(description = "Libellé du statut.") String statutDescription,
        @Schema(description = "Couleur du statut (nom .NET, ColorName).", example = "Orange") String statutCouleur,
        @Schema(description = "Personne à contacter (PersonToContact).") String personneAContacter,
        @Schema(description = "Montant HT.") BigDecimal montantHT,
        @Schema(description = "Montant de TVA.") BigDecimal montantTVA,
        @Schema(description = "Montant TTC.") BigDecimal montantTTC,
        @Schema(description = "Référence bureau d'ordre (OfficeOrderRef).") String referenceBureauOrdre
) {
}
