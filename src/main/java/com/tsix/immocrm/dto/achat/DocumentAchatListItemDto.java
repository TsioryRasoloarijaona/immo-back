package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Pièce d'achat (BuyDocument) dans la liste ; mêmes valeurs en tête de la fiche.")
public record DocumentAchatListItemDto(
        @Schema(description = "Id de la pièce (BuyDocument.PKey).") UUID id,
        @Schema(description = "N° de la pièce (Code).", example = "26/03017") String code,
        @Schema(description = "Nature stockée (DocType) : INVOICE, ORDER, QUOTE, DOCUMENT_TO_PAY, DELIVRY, REFUND.")
        String nature,
        @Schema(description = "Libellé de la nature.", example = "Facture") String natureLibelle,
        @Schema(description = "Référence fournisseur (DocRef).") String reference,
        @Schema(description = "Description de la pièce.") String description,
        @Schema(description = "Date de la pièce (DocDate).") LocalDate dateDocument,
        @Schema(description = "Date d'échéance (MaturityDate) ; null si absente.") LocalDate dateEcheance,
        @Schema(description = "Fournisseur (CfgTier.id).") UUID fournisseurId,
        @Schema(description = "Nom du fournisseur (CfgTier.Description).") String fournisseurNom,
        @Schema(description = "Projet de chantier (PrjProject.id) ; toujours dans le périmètre de l'appelant.") UUID projetId,
        @Schema(description = "Code du projet.") String projetCode,
        @Schema(description = "Lot de marché (PrjMarket.id) ; null si la pièce n'est pas rattachée à un lot.") UUID lotId,
        @Schema(description = "Code du lot ; null sans lot.") String lotCode,
        @Schema(description = "Code de la société (CfgCompany).") String societeCode,
        @Schema(description = "Statut (BuyDocumentStatus.id) ; null pour un devis.") UUID statutId,
        @Schema(description = "Code du statut ; null sans statut.") String statutCode,
        @Schema(description = "Libellé du statut ; null sans statut.") String statutDescription,
        @Schema(description = "Drapeau « OK pour paiement » du statut ; false sans statut.") boolean okPourPaiement,
        @Schema(description = "Montant HT (AmountHT).") BigDecimal montantHT,
        @Schema(description = "Montant de TVA (AmountVat).") BigDecimal montantTVA,
        @Schema(description = "Montant TTC (AmountTTC).") BigDecimal montantTTC,
        @Schema(description = "Montant à payer (AmountToPay), arrondi à 2 décimales : base du reste.") BigDecimal montantAPayer,
        @Schema(description = "Σ des lignes de règlement de la pièce, règlements annulés exclus.") BigDecimal montantRegle,
        @Schema(description = "Montant à payer − réglé ; négatif si la pièce est trop réglée.") BigDecimal resteARegler,
        @Schema(description = "Vrai si |reste| ≤ 0,05.") boolean solde,
        @Schema(description = "Vrai si le réglé dépasse de plus de 0,05 un à-payer positif ; faux pour une pièce négative.")
        boolean tropRegle,
        @Schema(description = "Vrai si le montant à payer est inférieur à −0,05 (récupération d'avance à déduire).")
        boolean aDeduire) {
}
