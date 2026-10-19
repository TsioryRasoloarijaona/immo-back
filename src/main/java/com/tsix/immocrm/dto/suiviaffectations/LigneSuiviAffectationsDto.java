package com.tsix.immocrm.dto.suiviaffectations;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneSuiviAffectations;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@JsonFilter(ColonneSuiviAffectations.FILTRE)
@Schema(description = "Une affectation de règlement à une échéance de vente (StlSettlementLine) ; seules les colonnes "
        + "demandées par ?colonnes= sont présentes.")
public record LigneSuiviAffectationsDto(
        @Schema(description = "StlSettlementLine.PKey.") UUID id,
        @Schema(description = "Date d'opération de la pièce, sinon sa date d'échéance ; null si aucune des deux.",
                example = "2025-03-31") LocalDate dateMouvement,
        @Schema(description = "Catégorie de l'échéance (ComSettlementCategory) ; null si l'échéance n'en a pas.")
        UUID categorieId,
        String categorieCode,
        String categorieLibelle,
        @Schema(description = "Société du projet de la tranche de l'article du dossier (pas la société de la pièce).")
        UUID entiteId,
        String entiteCode,
        String entiteLibelle,
        UUID projetId,
        String projetLibelle,
        @Schema(description = "Tranche de l'article du dossier ; null si le dossier n'a pas d'article.") UUID trancheId,
        String trancheCode,
        String trancheLibelle,
        @Schema(description = "StkItem.Code de l'article du dossier.") String articleCode,
        @Schema(description = "Tiers de la pièce (StlSettlement.TierId).") UUID tiersId,
        @Schema(description = "Nom affiché du tiers de la pièce.") String tiersNom,
        @Schema(description = "Montant de la ligne, négatif si la pièce est un débit (IsCredit = 0), positif sinon.",
                example = "-300") BigDecimal montantAffecte,
        @Schema(description = "DEBIT si StlSettlement.IsCredit = 0, CREDIT sinon (NULL compris).", example = "CREDIT")
        String sens,
        UUID dossierId,
        String dossierCode,
        @Schema(description = "StrStatus.Code du dossier, SANS_STATUT s'il n'en a pas.", example = "RESERVATION")
        String statutDossierCode,
        @Schema(description = "Libellé du statut du dossier, « Sans statut » s'il n'en a pas.")
        String statutDossierLibelle,
        @Schema(description = "StlSettlement.PKey de la pièce de règlement.") UUID pieceId,
        @Schema(description = "StlSettlement.Description.") String pieceLibelle,
        @Schema(description = "StlSettlement.Code (n° de chèque, de virement…).") String pieceReference,
        @Schema(description = "StlSettlement.ReceiptCode.") String recuReference,
        @Schema(description = "StlSettlement.DocDate.") LocalDate datePiece,
        @Schema(description = "StlSettlement.EcheanceDate.") LocalDate dateEcheancePiece,
        @Schema(description = "StlSettlement.SettlementDate.") LocalDate dateOperationPiece,
        @Schema(description = "IN_WORK, POINTED, CANCELED, ou SANS_ETAT si StlSettlement.Status est vide.",
                example = "IN_WORK") String etatPiece,
        @Schema(description = "StlSettlement.Amount, brut et non signé ; répété sur chaque ligne de la pièce.")
        BigDecimal montantPiece,
        UUID typePieceId,
        String typePieceCode,
        String typePieceLibelle,
        @Schema(description = "Nom affiché de la banque de la pièce (StlSettlement.CfgBankId).") String banqueNom,
        String compteCode,
        String compteLibelle,
        @Schema(description = "StlSettlement.Notes.") String notesPiece,
        @Schema(description = "ComDocument.DocDate de l'échéance.") LocalDate dateEcheance,
        @Schema(description = "ComDocument.Amount de l'échéance.") BigDecimal montantEcheance,
        @Schema(description = "ComDocument.Notes de l'échéance.") String notesEcheance) {
}
