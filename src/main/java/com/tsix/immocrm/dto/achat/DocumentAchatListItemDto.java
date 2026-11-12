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
        boolean aDeduire,
        @Schema(description = "Décompte lié (PrjWorkingOut.PKey, par PrjWorkingOut.BuyDocumentId) ; null sans décompte.")
        UUID decompteId,
        @Schema(description = "Code du décompte lié ; null sans décompte.") String decompteCode,
        @Schema(description = "N° d'ordre du décompte dans son lot (Sorting) ; null sans décompte.") Integer decompteNumero,
        @Schema(description = "Libellé de la société (CfgCompany.Description), colonne « Entité ».") String societeNom,
        @Schema(description = "Code du module de la pièce (AdmModule.Code) : BUY, PRJ, STL ; null sans module.",
                example = "BUY") String moduleCode,
        @Schema(description = "Libellé du module (AdmModule.Description) ; null sans module.", example = "Achat")
        String moduleLibelle,
        @Schema(description = "Type de lot (PrjMarketType.id) ; null sans type.") UUID typeLotId,
        @Schema(description = "Libellé du type de lot (colonne « Type ») ; null sans type.", example = "TRAVAUX")
        String typeLotLibelle,
        @Schema(description = "Archivée (IsClosed) ; NULL en base = false.") boolean archive,
        @Schema(description = "Comptabilisée (IsAccounted) ; NULL en base = false.") boolean comptabilise,
        @Schema(description = "Référence bureau d'ordre (OfficeOrderRef) ; null si absente.") String referenceBureauOrdre,
        @Schema(description = "Personne à contacter (PersonToContact) ; null si absente.") String personneAContacter,
        @Schema(description = "Dossier de groupement (BuyFolder.Description) ; null sans dossier.") String dossierGroupement,
        @Schema(description = "Catégories de ventilation (StlCategory.Description), séparées par « , » ; null sans ventilation.")
        String categories,
        @Schema(description = "Code de la devise (StlCurrency.Code) ; null sans devise.", example = "MAD") String deviseCode,
        @Schema(description = "Date d'alerte : toujours null, comme la vue OMRAN (null AS AlerteDate).") LocalDate dateAlerte,
        @Schema(description = "Bon de commande à clôturer : HT ≤ Σ HT de ses factures, statut non terminé ; false sinon.")
        boolean aCloturer,
        @Schema(description = "Couleur du statut (BuyDocumentStatus.ColorName, nom .NET) ; null sans statut.",
                example = "Green") String statutCouleur) {
}
