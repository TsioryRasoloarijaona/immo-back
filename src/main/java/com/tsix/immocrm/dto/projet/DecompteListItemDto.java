package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ligne de liste d'un décompte (PrjWorkingOut) : montants stockés, net recalculé et solde de sa facture.")
public record DecompteListItemDto(
        @Schema(description = "PrjWorkingOut.PKey.") UUID id,
        @Schema(description = "Code du décompte.", example = "26/00744") String code,
        @Schema(description = "Numéro du décompte dans son lot (Sorting).") Integer numero,
        @Schema(description = "Date du décompte (DocDate).") LocalDate dateDocument,
        @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Schema(description = "Code du lot.") String lotCode,
        @Schema(description = "Désignation du lot.") String lotDescription,
        @Schema(description = "Projet du lot (PrjProject.PKey).") UUID projetId,
        @Schema(description = "Code du projet.") String projetCode,
        @Schema(description = "Fournisseur : celui du décompte (CfgTierId), sinon celui du lot.") UUID fournisseurId,
        @Schema(description = "Nom du fournisseur, null si absent.") String fournisseurNom,
        @Schema(description = "HT de la période (AmountHT).") BigDecimal montantHT,
        @Schema(description = "TTC de la période (AmountTTC).") BigDecimal montantTTC,
        @Schema(description = "Travaux supplémentaires TTC (extraAmount).") BigDecimal travauxSupplementaires,
        @Schema(description = "Retenue de garantie de la période (WarrantyAmount).") BigDecimal retenueGarantie,
        @Schema(description = "Net à payer stocké par OMRAN (AmountToPay).") BigDecimal netAPayer,
        @Schema(description = "Net à payer recalculé par Arpent sur les colonnes stockées, 2 décimales.")
        BigDecimal netAPayerRecalcule,
        @Schema(description = "netAPayer − netAPayerRecalcule, signé.") BigDecimal ecartNetAPayer,
        @Schema(description = "Vrai si |ecartNetAPayer| > 0,02 : un écart s'affiche, il ne bloque rien.") boolean ecartSignificatif,
        @Schema(description = "Cumul par lot du net de RG (AmountTTCAfterDeductions), sans les extras.")
        BigDecimal cumulNetRetenueGarantie,
        @Schema(description = "Dernier décompte du lot (FinalWorkingOut).") boolean dernier,
        @Schema(description = "Facture du décompte (PrjWorkingOut.BuyDocumentId), null si non facturé.") UUID factureId,
        @Schema(description = "Code de la facture.") String factureCode,
        @Schema(description = "Code du statut de la facture (BuyDocumentStatus).") String factureStatutCode,
        @Schema(description = "Libellé du statut de la facture.") String factureStatutDescription,
        @Schema(description = "Σ des règlements non annulés de la facture ; 0 sans facture.") BigDecimal montantRegle,
        @Schema(description = "À payer de la facture (BuyDocument.AmountToPay) − réglé ; 0 sans facture.") BigDecimal resteARegler
) {
}
