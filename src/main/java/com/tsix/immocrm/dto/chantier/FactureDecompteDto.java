package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Facture fournisseur générée depuis le décompte (BuyDocument, DocType INVOICE).")
public record FactureDecompteDto(
        @Schema(description = "Identifiant de la facture.") UUID id,
        @Schema(description = "Code de la facture.", example = "26/03017") String code,
        @Schema(description = "Date de la facture.") LocalDate dateDocument,
        @Schema(description = "Date d'échéance (MaturityDate).") LocalDate dateEcheance,
        @Schema(description = "Montant HT.") BigDecimal montantHT,
        @Schema(description = "Montant TTC.") BigDecimal montantTTC,
        @Schema(description = "Net à payer de la facture (AmountToPay) = net du décompte + régularisation.")
        BigDecimal montantAPayer,
        @Schema(description = "Régularisation (AmountRegul).") BigDecimal regularisation,
        @Schema(description = "Code du statut.", example = "30") String statutCode,
        @Schema(description = "Libellé du statut.", example = "VALIDÉ DG") String statutDescription,
        @Schema(description = "Statut autorisant le paiement (BuyDocumentStatus.IsOkForPayement).") boolean okPourPaiement) {
}
