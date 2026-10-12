package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Décompte (PrjWorkingOut) : montants stockés tels quels, facture et état de paiement.")
public record DecompteListItemDto(
        @Schema(description = "Identifiant du décompte.") UUID id,
        @Schema(description = "Code du décompte.", example = "26/00744") String code,
        @Schema(description = "Numéro d'ordre dans le lot (Sorting).", example = "5") Integer numero,
        @Schema(description = "Date du décompte (DocDate).") LocalDate dateDocument,
        @Schema(description = "Lot du décompte.") UUID lotId,
        @Schema(description = "Code du lot.") String lotCode,
        @Schema(description = "Désignation du lot.") String lotDescription,
        @Schema(description = "Projet du lot.") UUID projetId,
        @Schema(description = "Code du projet.") String projetCode,
        @Schema(description = "Fournisseur du décompte (CfgTierId).") UUID fournisseurId,
        @Schema(description = "Nom du fournisseur.") String fournisseurNom,
        @Schema(description = "HT de la période (AmountHT) ; 0 pour un décompte saisi en travaux supplémentaires seuls.")
        BigDecimal montantHT,
        @Schema(description = "TTC de la période (AmountTTC).") BigDecimal montantTTC,
        @Schema(description = "Travaux supplémentaires, montant TTC (extraAmount).") BigDecimal travauxSupplementaires,
        @Schema(description = "Retenue de garantie de la période (WarrantyAmount).") BigDecimal retenueGarantie,
        @Schema(description = "Net à payer du décompte (AmountToPay).") BigDecimal netAPayer,
        @Schema(description = "Cumul du lot, net de RG, sans les extras (AmountTTCAfterDeductions).")
        BigDecimal cumulNetRetenueGarantie,
        @Schema(description = "Dernier décompte du lot (FinalWorkingOut).") boolean dernier,
        @Schema(description = "Facture du décompte (BuyDocumentId) ; null si non facturé.") UUID factureId,
        @Schema(description = "Code de la facture.") String factureCode,
        @Schema(description = "Code du statut de la facture (BuyDocumentStatus).") String factureStatutCode,
        @Schema(description = "Libellé du statut de la facture.") String factureStatutDescription,
        @Schema(description = "Règlements non annulés de la facture ; 0 si non facturé.") BigDecimal montantRegle,
        @Schema(description = "Net à payer de la facture − réglé ; 0 si non facturé.") BigDecimal resteARegler) {
}
