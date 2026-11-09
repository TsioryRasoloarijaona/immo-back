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
        @Schema(description = "Règlements de la pièce, annulés compris, par date puis code.") List<ReglementDocumentAchatDto> reglements) {

    @Schema(description = "Ligne de la pièce (BuyDocumentLine), colonnes stockées.")
    public record LigneDocumentAchatDto(
            @Schema(description = "Id de la ligne.") UUID id,
            @Schema(description = "Ordre (Sorting).") Integer ordre,
            @Schema(description = "Désignation (Description).") String designation,
            @Schema(description = "Unité (Unity).") String unite,
            @Schema(description = "Quantité.") BigDecimal quantite,
            @Schema(description = "Prix unitaire HT.") BigDecimal prixUnitaireHT,
            @Schema(description = "Taux de TVA en % (VatRatio × 100).", example = "20") BigDecimal tauxTVA,
            @Schema(description = "Remise en % (DiscountRatio × 100).", example = "0") BigDecimal remisePourcent) {
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
            @Schema(description = "Note saisie au passage.") String note) {
    }

    @Schema(description = "Ligne de règlement de la pièce et son règlement (StlSettlement).")
    public record ReglementDocumentAchatDto(
            @Schema(description = "Id du règlement (StlSettlement.id).") UUID id,
            @Schema(description = "Code du règlement.") String code,
            @Schema(description = "Date du règlement (StlSettlement.DocDate).") LocalDate dateDocument,
            @Schema(description = "Code du type de règlement.") String typeCode,
            @Schema(description = "Libellé du type de règlement.") String typeDescription,
            @Schema(description = "Montant de la ligne affecté à la pièce.") BigDecimal montant,
            @Schema(description = "Vrai si le règlement est annulé (CANCELED) : listé, exclu du réglé.") boolean annule) {
    }
}
