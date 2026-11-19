package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Fiche d'une demande interne d'achat : onglets Principal, Modalités / dates et Autres.")
public record DemandeAchatFiche(
        @Schema(description = "BuySelection.PKey.") UUID id,
        @Schema(description = "Code yy/nnnnn, non modifiable.") String code,
        @Schema(description = "Date de la demande (DocDate).") LocalDate date,
        @Schema(description = "Description.") String description,
        @Schema(description = "Description longue (DescriptionLong, 500 caractères).") String descriptionLongue,
        @Schema(description = "Procédure d'achat.") ElementDemandeAchat procedure,
        @Schema(description = "Statut stocké.") StatutDemandeAchat statut,
        @Schema(description = "Société (CfgCompany).") ElementDemandeAchat societe,
        @Schema(description = "Projet ; null sans projet.") ElementDemandeAchat projet,
        @Schema(description = "Lot de marché ; null sans lot.") ElementDemandeAchat lot,
        @Schema(description = "Type de lot (PrjMarketType) ; null si absent.") ElementDemandeAchat typeLot,
        @Schema(description = "Catégorie (StlCategory) ; null si absente.") ElementDemandeAchat categorie,
        @Schema(description = "Devise (StlCurrency) ; null si absente.") ElementDemandeAchat devise,
        @Schema(description = "Module (AdmModule).") ElementDemandeAchat module,
        @Schema(description = "Déclencheur (employé, CfgTier) ; null si absent.") ElementDemandeAchat declencheur,
        @Schema(description = "Fournisseur retenu, en lecture seule ; null sans offre sélectionnée.") ElementDemandeAchat fournisseur,
        @Schema(description = "TVA par défaut (DefaultVat).") BigDecimal tvaDefaut,
        @Schema(description = "Prix estimé HT (PriceEstimatedHT).") BigDecimal prixEstimeHT,
        @Schema(description = "Prix estimé TTC (PriceEstimatedTTC), contrôlé par la procédure (R6).") BigDecimal prixEstimeTTC,
        @Schema(description = "Mode de paiement (StlSettlementType) ; null si absent.") ElementDemandeAchat paiement,
        @Schema(description = "Délai de paiement en jours (PaymentDelay).") Integer delaiPaiement,
        @Schema(description = "Dates d'appel d'offre.") DatesDemandeAchat dates,
        @Schema(description = "Notes (1000 caractères).") String notes,
        @Schema(description = "Texte de la lettre de sélection (TextLetterSelection).") String texteLettre,
        @Schema(description = "Dernière demande de son groupe ; false = toute écriture répond 409.") boolean estDerniere,
        @Schema(description = "Procédure modifiable : statut stocké de type Nouveau et demande dernière (R5).") boolean procedureModifiable,
        @Schema(description = "Phases ouvertes par le statut.") PhasesDemandeAchat phases,
        @Schema(description = "Procédure confidentielle (IsConfidential) : sous-onglets masqués sans le droit.") boolean confidentielle,
        @Schema(description = "Lot multi-fournisseur (PrjMarket.IsMultiSupplier) ; false sans lot.") boolean lotMultiFournisseur) {
}
