package com.tsix.immocrm.dto.achat;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche d'une pièce d'achat : les champs de la ligne de liste, puis l'en-tête et les onglets.")
public record DocumentAchatDetailDto(
        @JsonUnwrapped DocumentAchatListItemDto document,
        @Schema(description = "Notes de la pièce.") String notes,
        @Schema(description = "Régularisation (AmountRegul).") BigDecimal regularisation,
        @Schema(description = "Timbre fiscal (FiscalStamp).") BigDecimal timbreFiscal,
        @Schema(description = "Drapeau « Validé comptabilité » du statut ; false sans statut.") boolean valideComptabilite,
        @Schema(description = "Code de l'entité (StrEntity).") String entiteCode,
        @Schema(description = "Libellé de l'entité (StrEntity).") String entiteDescription,
        @Schema(description = "Date de création (UserCreateDateTime).") LocalDateTime dateCreation,
        @Schema(description = "Auteur de la création (UserCreate, tel que stocké).") String creePar,
        @Schema(description = "Date de dernière modification (UserUpdateDateTime).") LocalDateTime dateModification,
        @Schema(description = "Auteur de la dernière modification (UserUpdate, tel que stocké).") String modifiePar,
        @Schema(description = "Lignes de la pièce, par ordre (Sorting).") List<LigneDocumentAchatDto> lignes,
        @Schema(description = "Ventilation par catégorie, par code de catégorie.") List<VentilationDocumentAchatDto> ventilation,
        @Schema(description = "Historique des statuts, du plus ancien au plus récent.") List<HistoriqueDocumentAchatDto> historique,
        @Schema(description = "Règlements de la pièce, annulés compris, par date puis code.") List<ReglementDocumentAchatDto> reglements,
        @Schema(description = "Déclencheur (CfgTierTriggerId) ; null si absent.") UUID declencheurId,
        @Schema(description = "Nom du déclencheur (CfgTier.Description).") String declencheurNom,
        @Schema(description = "Date de livraison souhaitée (PrevisionDeliveryDate), toute nature.") LocalDate dateLivraisonSouhaitee,
        @Schema(description = "Seconde note (Notes2).") String note2,
        @Schema(description = "Devis sélectionné (IsSelected) ; false si absent.") boolean devisSelectionne,
        @Schema(description = "Σ TTC des avoirs visibles de la pièce (InvoiceSourceId) ; 0 sans avoir, jamais soustrait.")
        BigDecimal avoirsMontant,
        @Schema(description = "Retenues et avances (colonne droite), lues telles quelles.") RetenuesAvancesAchatDto retenuesAvances,
        @Schema(description = "Devis d'origine (BuyQuoteId) ; null si absent ou hors périmètre.") UUID devisOrigineId,
        @Schema(description = "Code du devis d'origine ; null si absent ou hors périmètre.") String devisOrigineCode,
        @Schema(description = "Bon de commande rattaché (BuyOrderId) ; null si absent ou non visible.") UUID bonCommandeId,
        @Schema(description = "Code du bon de commande rattaché ; null si absent ou non visible.") String bonCommandeCode,
        @Schema(description = "Facture source d'un avoir (InvoiceSourceId) ; null si absente ou hors périmètre.") UUID factureSourceId,
        @Schema(description = "Code de la facture source ; null si absente ou hors périmètre.") String factureSourceCode,
        @Schema(description = "Lignes groupées par taux de TVA, taux croissant ; vide sans ligne.") List<RecapTvaAchatDto> recapTva,
        @Schema(description = "Ventilation par projet, par code de projet, non filtrée par périmètre.")
        List<VentilationProjetAchatDto> ventilationProjets,
        @Schema(description = "Bons de livraison visibles d'un bon de commande (BuyOrderId) ; vide pour une autre nature.")
        List<PieceLieeAchatDto> bonsLivraison,
        @Schema(description = "Factures visibles d'un bon de commande (BuyOrderId ∪ BuyOrderInvoice) ; vide sinon.")
        List<PieceLieeAchatDto> factures,
        @Schema(description = "Avoirs visibles d'une facture (InvoiceSourceId), avec leurs lignes ; vide pour une autre nature.")
        List<AvoirAchatDto> avoirs,
        @Schema(description = "Actions d'écriture ouvertes au compte (boutons de la fiche).") ActionsPieceAchatDto actions) {

    @Schema(description = "Ligne de la pièce (BuyDocumentLine) : colonnes stockées puis calculées (vue OMRAN BuyDocumentLineView).")
    public record LigneDocumentAchatDto(
            @Schema(description = "Id de la ligne.") UUID id,
            @Schema(description = "Ordre (Sorting).") Integer ordre,
            @Schema(description = "Désignation (Description).") String designation,
            @Schema(description = "Unité (Unity).") String unite,
            @Schema(description = "Quantité.") BigDecimal quantite,
            @Schema(description = "Prix unitaire HT.") BigDecimal prixUnitaireHT,
            @Schema(description = "Taux de TVA en % (VatRatio × 100).", example = "20") BigDecimal tauxTVA,
            @Schema(description = "Remise en % (DiscountRatio × 100).", example = "0") BigDecimal remisePourcent,
            @Schema(description = "Code de la ligne ; null possible.") String code,
            @Schema(description = "FODEC en % (FODECRatio × 100) ; 0 si absent.", example = "0") BigDecimal fodecPourcent,
            @Schema(description = "Prix unitaire TTC stocké (UnitPriceTTC).") BigDecimal prixUnitaireTTC,
            @Schema(description = "PU HT net : PU HT × (1 − remise) × (1 + FODEC), sans arrondi.") BigDecimal prixUnitaireHTNet,
            @Schema(description = "Total HT : PU HT net × quantité, sans arrondi.") BigDecimal totalHT,
            @Schema(description = "Total TVA : total HT × taux de TVA.") BigDecimal totalTVA,
            @Schema(description = "Total TTC : total HT + total TVA.") BigDecimal totalTTC) {
    }

    @Schema(description = "Ventilation de la pièce sur une catégorie (BuyVentilationCategory) : le seul taux est stocké.")
    public record VentilationDocumentAchatDto(
            @Schema(description = "Code de la catégorie (StlCategory).") String categorieCode,
            @Schema(description = "Libellé de la catégorie.") String categorieDescription,
            @Schema(description = "Taux de ventilation, fraction (0,75 = 75 %).", example = "0.75") BigDecimal taux) {
    }

    @Schema(description = "Passage de la pièce dans un statut (BuyDocumentStatusHistory).")
    public record HistoriqueDocumentAchatDto(
            @Schema(description = "Date du passage.") LocalDateTime date,
            @Schema(description = "Code du statut atteint.") String statutCode,
            @Schema(description = "Libellé du statut atteint.") String statutDescription,
            @Schema(description = "Utilisateur (AdmUser.Description, à défaut Code).") String utilisateur,
            @Schema(description = "Note saisie au passage.") String note,
            @Schema(description = "Transition empruntée (BuyDocumentWorkFlow.Description) ; null à la création.") String transition) {
    }

    @Schema(description = "Règlement de la pièce (StlSettlement) ; pour un bon de commande, un rang par règlement de ses factures "
            + "et avances, avec le pointage.")
    public record ReglementDocumentAchatDto(
            @Schema(description = "Id du règlement (StlSettlement.id).") UUID id,
            @Schema(description = "Code du règlement.") String code,
            @Schema(description = "Date du règlement (StlSettlement.DocDate).") LocalDate dateDocument,
            @Schema(description = "Code du type de règlement.") String typeCode,
            @Schema(description = "Libellé du type de règlement.") String typeDescription,
            @Schema(description = "Montant de la ligne affecté à la pièce ; pour un bon de commande, montant du règlement.")
            BigDecimal montant,
            @Schema(description = "Vrai si le règlement est annulé (CANCELED) : listé, exclu du réglé.") boolean annule,
            @Schema(description = "État du règlement (Status) ; bon de commande seulement, null sinon.") String etat,
            @Schema(description = "Date de pointage (SettlementDate) ; bon de commande seulement.") LocalDate datePointage,
            @Schema(description = "Date de pointage, à défaut échéance ; bon de commande seulement.") LocalDate datePointagePrevue,
            @Schema(description = "Prévue échue et règlement non pointé (POINTED) ; null hors bon de commande.")
            Boolean pointageReporte,
            @Schema(description = "Prévue future : prévue ; échue : prévue si pointé, sinon aujourd'hui ; bon de commande seulement.")
            LocalDate datePointageReportee,
            @Schema(description = "Montant brut du règlement (AmountBase) ; bon de commande seulement.") BigDecimal montantBrut) {

        /** Rang hors bon de commande : sans pointage. */
        public ReglementDocumentAchatDto(UUID id, String code, LocalDate dateDocument, String typeCode,
                                         String typeDescription, BigDecimal montant, boolean annule) {
            this(id, code, dateDocument, typeCode, typeDescription, montant, annule, null, null, null, null, null, null);
        }
    }
}
