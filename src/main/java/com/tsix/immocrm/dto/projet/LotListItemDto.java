package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ligne de la liste des lots (PrjMarket, « marché » dans OMRAN) ; montants calculés côté serveur.")
public record LotListItemDto(
        @Schema(description = "PrjMarket.PKey.") UUID id,
        @Schema(description = "Code du lot.", example = "24/00033") String code,
        @Schema(description = "Désignation du lot.") String description,
        @Schema(description = "Projet du lot (PrjProject.PKey).") UUID projetId,
        @Schema(description = "Code du projet.", example = "ARS-PRJ") String projetCode,
        @Schema(description = "Code du type de lot (PrjMarketType), null si absent.") String typeCode,
        @Schema(description = "Libellé du type de lot, null si absent.") String typeDescription,
        @Schema(description = "Code du statut (StrStatus) ; étiquette, jamais une règle.") String statutCode,
        @Schema(description = "Libellé du statut.") String statutDescription,
        @Schema(description = "Ordre du statut dans son workflow (StrStatus.Sorting).") Integer statutOrdre,
        @Schema(description = "Fournisseur du lot (CfgTier.PKey), null si absent.") UUID fournisseurId,
        @Schema(description = "Nom du fournisseur, null si absent.") String fournisseurNom,
        @Schema(description = "Code de la catégorie (StlCategory), null si absente.") String categorieCode,
        @Schema(description = "Libellé de la catégorie, null si absente.") String categorieDescription,
        @Schema(description = "Date de début contractuelle (StartDateManual).") LocalDate dateDebut,
        @Schema(description = "Date de fin contractuelle (EndDateManual).") LocalDate dateFin,
        @Schema(description = "Délai contractuel en jours (DelayManual).") Integer delaiJours,
        @Schema(description = "Lot multi-fournisseur (IsMultiSupplier).") boolean multiFournisseur,
        @Schema(description = "Marché HT : Σ bordereau définitif, sinon montant forfaitaire.") BigDecimal montantHT,
        @Schema(description = "Marché TVA : Σ bordereau définitif, sinon montant forfaitaire.") BigDecimal montantTVA,
        @Schema(description = "Marché TTC : Σ bordereau définitif, sinon montant forfaitaire.") BigDecimal montantTTC,
        @Schema(description = "Vrai si le marché vient du bordereau, faux s'il est forfaitaire (lot sans ligne).")
        boolean montantsDepuisBordereau,
        @Schema(description = "Taux de retenue de garantie du lot en pourcentage (0–100).", example = "10")
        BigDecimal tauxRetenueGarantie,
        @Schema(description = "Nombre de décomptes du lot.") long nbDecomptes,
        @Schema(description = "Σ AmountHT des décomptes.") BigDecimal cumulDecomptesHT,
        @Schema(description = "Σ AmountTTC des décomptes.") BigDecimal cumulDecomptesTTC,
        @Schema(description = "Σ des travaux supplémentaires TTC des décomptes (extraAmount).") BigDecimal cumulTravauxSupplementaires,
        @Schema(description = "Σ des retenues de garantie des décomptes (WarrantyAmount).") BigDecimal cumulRetenueGarantie,
        @Schema(description = "Cumul décomptes TTC / marché TTC × 100, 2 décimales ; null si marché nul ; non plafonné.")
        BigDecimal avancementPct,
        @Schema(description = "Σ à payer des factures des décomptes (BuyDocument.AmountToPay).") BigDecimal totalFacture,
        @Schema(description = "Σ des règlements non annulés de ces factures.") BigDecimal totalRegle,
        @Schema(description = "Facturé − réglé.") BigDecimal resteARegler,
        @Schema(description = "Avancement OMRAN (Achievement), en fraction : 0,42 = 42 %.", example = "0.42") BigDecimal avancement,
        @Schema(description = "Coût prévisionnel HT (CostHT d'OMRAN).") BigDecimal coutPrevisionnelHT,
        @Schema(description = "Coût prévisionnel TTC (CostTTC d'OMRAN).") BigDecimal coutPrevisionnelTTC,
        @Schema(description = "Net à payer prévisionnel (CostToPay d'OMRAN).") BigDecimal coutPrevisionnelNetAPayer,
        @Schema(description = "Σ des lignes de règlement des pièces d'achat du lot, annulées comprises (AmountSettled).")
        BigDecimal coutPrevisionnelRegle,
        @Schema(description = "coutPrevisionnelNetAPayer − coutPrevisionnelRegle.") BigDecimal coutPrevisionnelReste,
        @Schema(description = "Nom de couleur .NET du statut (StrStatus.ColorName), null si absent.", example = "LimeGreen")
        String statutCouleur,
        @Schema(description = "Genre du lot (StrEntityId), null si inconnu.",
                allowableValues = {"ENTREPRENEURS", "FOURNISSEURS", "ADMINISTRATION", "MAITRES_OEUVRE", "CONCESSIONNAIRES"})
        String genre,
        @Schema(description = "Autres notes du lot (PrjMarket.Notes).") String notes
) {
}
