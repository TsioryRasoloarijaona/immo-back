package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.domain.projet.NaturePieceLot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Pré-remplissage d'une pièce à payer du lot, calculé côté serveur.")
public record PieceLotModeleDto(
        @Schema(description = "Nature demandée.") NaturePieceLot nature,
        @Schema(description = "Date proposée : aujourd'hui.") LocalDate date,
        @Schema(description = "Échéance proposée : date + délai de paiement du lot (sinon du fournisseur).") LocalDate echeance,
        @Schema(description = "Référence (DocRef) qui sera donnée : AVA n, W.AVA n, RRG n ou RCP.", example = "AVA2") String reference,
        @Schema(description = "Montant TTC proposé (reste calculé, plancher 0 ; 0 pour une avance sur décompte).")
        BigDecimal montantTTC,
        @Schema(description = "Notes proposées : libellé, lot et projet.") String notes,
        @Schema(description = "Fournisseur du lot, null si aucun.") String fournisseur,
        @Schema(description = "Reste calculé, indicatif (saisie non plafonnée) ; null pour W.AVA et RCP.") BigDecimal montantMaximum
) {
}
