package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ligne de règlement d'une facture de décompte ; une ligne annulée est listée mais ne compte pas.")
public record ReglementFactureDto(
        @Schema(description = "Identifiant du règlement (StlSettlement).") UUID id,
        @Schema(description = "Code du règlement ; souvent vide en base.") String code,
        @Schema(description = "Date du règlement (StlSettlement.DocDate ; SettlementDate est presque toujours vide).")
        LocalDate dateDocument,
        @Schema(description = "Date d'échéance (EcheanceDate).") LocalDate dateEcheance,
        @Schema(description = "Montant de la ligne affecté à cette facture.") BigDecimal montant,
        @Schema(description = "Code du mode de règlement (StlSettlementType).") String typeCode,
        @Schema(description = "Libellé du mode de règlement.", example = "VIREMENT") String typeDescription,
        @Schema(description = "Vrai si le règlement est annulé (Status CANCELED) : exclu de montantRegle.") boolean annule) {
}
