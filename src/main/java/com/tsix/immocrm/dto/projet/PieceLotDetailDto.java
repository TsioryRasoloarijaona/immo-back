package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.domain.projet.NaturePieceLot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Fiche d'une pièce à payer du lot (BuyDocument DOCUMENT_TO_PAY).")
public record PieceLotDetailDto(
        @Schema(description = "BuyDocument.PKey.") UUID id,
        @Schema(description = "Nature (ReferenceType).") NaturePieceLot nature,
        @Schema(description = "Code de la pièce (séquence « Document à régler entrant »).") String code,
        @Schema(description = "Référence (DocRef).", example = "AVA1") String reference,
        @Schema(description = "Date de la pièce (DocDate).") LocalDate date,
        @Schema(description = "Échéance (MaturityDate).") LocalDate echeance,
        @Schema(description = "Montant HT (Σ des lignes).") BigDecimal montantHT,
        @Schema(description = "Montant de TVA.") BigDecimal montantTVA,
        @Schema(description = "Montant TTC.") BigDecimal montantTTC,
        @Schema(description = "Pénalité sur R.G. (− AmountRegul), MARKET_WARRANTY seulement ; null sinon.") BigDecimal penaliteRg,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Fournisseur de la pièce, null si aucun.") String fournisseur,
        @Schema(description = "Libellé du statut de la pièce, null si aucun.") String statut,
        @Schema(description = "Faux si le statut du lot n'a pas le drapeau de la nature ou si le statut de la pièce est en lecture seule.")
        boolean modifiable,
        @Schema(description = "Vrai si la suppression passerait : droit, statut non verrouillé, aucun règlement.") boolean supprimable
) {
}
