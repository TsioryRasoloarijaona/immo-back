package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Affectation d'un règlement fournisseur (StlSettlementLine) à une facture d'un projet visible.")
public record ReglementProjetDto(
        @Schema(description = "Id de la ligne d'affectation (StlSettlementLine.PKey).") UUID id,
        @Schema(description = "Règlement (StlSettlement.PKey).") UUID reglementId,
        @Schema(description = "N° du règlement.") String reglementCode,
        @Schema(description = "Date du règlement (DocDate).") LocalDate dateReglement,
        @Schema(description = "Date d'échéance du règlement (EcheanceDate) ; null si absente.") LocalDate dateEcheance,
        @Schema(description = "Code du type de règlement ; null sans type.") String typeCode,
        @Schema(description = "Libellé du type de règlement ; null sans type.") String typeDescription,
        @Schema(description = "Montant affecté à la facture (StlSettlementLine.Amount), brut.") BigDecimal montant,
        @Schema(description = "Vrai si le règlement est annulé (CANCELED) : la ligne n'entre dans aucun réglé.") boolean annule,
        @Schema(description = "Facture réglée (BuyDocument.PKey).") UUID factureId,
        @Schema(description = "N° de la facture.") String factureCode,
        @Schema(description = "Nom du fournisseur de la facture.") String fournisseurNom,
        @Schema(description = "Projet de la facture (PrjProject.PKey).") UUID projetId,
        @Schema(description = "Code du projet.") String projetCode,
        @Schema(description = "Lot de la facture (PrjMarket.PKey) ; null hors lot.") UUID lotId,
        @Schema(description = "Code du lot ; null hors lot.") String lotCode,
        @Schema(description = "Décompte lié à la facture (PrjWorkingOut.PKey) ; null sans décompte.") UUID decompteId,
        @Schema(description = "Code du décompte ; null sans décompte.") String decompteCode) {
}
