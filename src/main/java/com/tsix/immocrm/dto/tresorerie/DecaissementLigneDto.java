package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Un décaissement (StlSettlement, IsCredit = 0) de la liste Dépenses › Décaissements.")
public record DecaissementLigneDto(
        @Schema(description = "StlSettlement.PKey.") UUID id,
        @Schema(description = "Référence (Code, sinon code reçu).", example = "DEC-000123") String code,
        String description,
        @Schema(description = "Date de la pièce (DocDate).") LocalDate datePiece,
        @Schema(description = "Échéance (EcheanceDate).") LocalDate echeance,
        @Schema(description = "Date de pointage (SettlementDate) ; null = non pointé.") LocalDate datePointage,
        @Schema(description = "Date de pointage reportée, calculée par le serveur (voir dateSur=POINTAGE_REPORTE).")
        LocalDate dateMouvement,
        @Schema(description = "Libellé du type de règlement.", example = "Virement") String type,
        @Schema(description = "Nom du tiers réglé.") String tiers,
        @Schema(description = "Banque du tiers (CfgTier de CfgBankId).") String banque,
        @Schema(description = "Compte bancaire de la société (StlAccount).") String compte,
        @Schema(description = "Code du statut tel que stocké ; null si absent.", example = "IN_WORK") String statut,
        @Schema(description = "Libellé français du statut.", example = "En cours") String statutLibelle,
        @Schema(description = "Montant tel que stocké (Amount), sans changement de signe.") BigDecimal montant,
        @Schema(description = "Σ des montants de toutes ses lignes d'affectation, visibles ou non.") BigDecimal montantAffecte,
        @Schema(description = "AmountBase × ConvertValue / ConvertUnit (1 si unité 0) − montantAffecte.")
        BigDecimal resteAAffecter,
        @Schema(description = "Libellé de la société.") String societe,
        @Schema(description = "IsAccounted.") boolean comptabilise,
        @Schema(description = "true : aucune ligne d'affectation (badge « Non affecté »).") boolean nonAffecte,
        @Schema(description = "Échéance ≤ aujourd'hui et statut ≠ POINTED (icône d'alerte OMRAN).") boolean enRetard) {
}
