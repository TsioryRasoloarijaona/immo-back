package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Modalité de paiement de la demande (BuySelectionPaymentTerm) ; même forme en lecture et en saisie.")
public record ModaliteDemandeAchat(
        @Schema(description = "PKey ; ignoré à la saisie.") UUID id,
        @Schema(description = "O.D.T. : ordre d'affichage (Sorting), obligatoire.") Integer ordre,
        @Schema(description = "Code, obligatoire (255 caractères au plus).") String code,
        @Schema(description = "Ratio du montant, enregistré tel que saisi (fraction, comme PaymentRatio).") BigDecimal ratio) {
}
