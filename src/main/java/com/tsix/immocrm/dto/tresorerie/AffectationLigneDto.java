package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Une affectation (StlSettlementLine) d'un règlement à une pièce visible.")
public record AffectationLigneDto(
        @Schema(description = "StlSettlementLine.PKey.") UUID id,
        @Schema(description = "StlSettlement.PKey : ouvre la fiche du décaissement.") UUID reglementId,
        @Schema(description = "Référence du règlement.") String code,
        String description,
        @Schema(description = "Date de la pièce du règlement.") LocalDate datePiece,
        @Schema(description = "Échéance du règlement.") LocalDate echeance,
        @Schema(description = "Date de pointage ; null = non pointé.") LocalDate datePointage,
        @Schema(description = "Date de pointage reportée, calculée par le serveur.") LocalDate dateMouvement,
        @Schema(description = "Code reçu (ReceiptCode).") String codeRecu,
        @Schema(description = "Code du statut du règlement.", example = "POINTED") String statut,
        @Schema(description = "Libellé français du statut.", example = "Pointé") String statutLibelle,
        @Schema(description = "Montant du règlement tel que stocké.") BigDecimal montantReglement,
        String tiers,
        @Schema(description = "Libellé du type de règlement.") String type,
        String banque,
        String compte,
        @Schema(description = "Code du module du règlement.", example = "PRJ") String module,
        @Schema(description = "Catégorie de trésorerie (StlCategory) du règlement.") String categorie,
        String societe,
        @Schema(description = "Code du projet de la pièce.") String projet,
        @Schema(description = "Toujours null ici : tranche des pièces de vente, hors périmètre.") String tranche,
        @Schema(description = "Toujours null ici : article des pièces de vente, hors périmètre.") String article,
        @Schema(description = "Code du lot de marché de la pièce.") String lot,
        @Schema(description = "Pièce réglée ; ouvre la facture d'achat.") PieceTresorerieDto pieceLiee,
        @Schema(description = "Net à payer de la pièce (AmountToPay).") BigDecimal montantPiece,
        @Schema(description = "Montant affecté (StlSettlementLine.Amount).") BigDecimal montantAffecte,
        @Schema(description = "Toujours null ici : dossier de vente, hors périmètre.") String dossierCode,
        @Schema(description = "Toujours null ici : statut du dossier de vente, hors périmètre.") String dossierStatut) {
}
