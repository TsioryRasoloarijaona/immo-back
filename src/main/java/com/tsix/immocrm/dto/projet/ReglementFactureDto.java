package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ligne de règlement (StlSettlementLine) portée par une facture de décompte.")
public record ReglementFactureDto(
        @Schema(description = "Règlement (StlSettlement.PKey).") UUID id,
        @Schema(description = "Code du règlement.") String code,
        @Schema(description = "Date du règlement (StlSettlement.DocDate).") LocalDate dateDocument,
        @Schema(description = "Date d'échéance (EcheanceDate).") LocalDate dateEcheance,
        @Schema(description = "Montant de la ligne affecté à la facture.") BigDecimal montant,
        @Schema(description = "Code du type de règlement (StlSettlementType).", example = "VIREMENT") String typeCode,
        @Schema(description = "Libellé du type de règlement.") String typeDescription,
        @Schema(description = "Règlement annulé (Status CANCELED) : listé, exclu du réglé.") boolean annule
) {
}
