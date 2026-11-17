package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.domain.projet.NaturePieceLot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Saisie d'une pièce à payer du lot (avance sur marché ou décompte, restitution de RG, remboursement de prorata).")
public record PieceLotSaisieDto(
        @Schema(description = "Nature de la pièce (ReferenceType) ; obligatoire à la création, ignorée en modification.")
        NaturePieceLot nature,
        @Schema(description = "Date de la pièce (DocDate), obligatoire.") LocalDate date,
        @Schema(description = "Échéance (MaturityDate) ≥ date ; absente = date + délai de paiement du lot.") LocalDate echeance,
        @Schema(description = "Montant TTC ≥ 0 ; absent à la création = montant par défaut du modèle.") BigDecimal montantTTC,
        @Schema(description = "Notes de la pièce.") String notes,
        @Schema(description = "Pénalité sur R.G. ≥ 0 (MARKET_WARRANTY seulement, ignorée sinon) ; écrite en AmountRegul négatif.")
        BigDecimal penaliteRg
) {
}
