package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Lot d'un projet (PrjMarket, « marché » en base) avec ses montants recalculés et son état de paiement.")
public record LotListItemDto(
        @Schema(description = "Identifiant du lot (PrjMarket.PKey).") UUID id,
        @Schema(description = "Code du lot.", example = "24/00033") String code,
        @Schema(description = "Désignation du lot.") String description,
        @Schema(description = "Code du type de lot (PrjMarketType).", example = "20") String typeCode,
        @Schema(description = "Libellé du type de lot.", example = "TRAVAUX") String typeDescription,
        @Schema(description = "Code du statut (étiquette non tenue à jour, jamais une règle).") String statutCode,
        @Schema(description = "Libellé du statut.") String statutDescription,
        @Schema(description = "Ordre du statut (StrStatus.Sorting).") Integer statutOrdre,
        @Schema(description = "Fournisseur titulaire (CfgSupplierId) ; null si aucun.") UUID fournisseurId,
        @Schema(description = "Nom du fournisseur (CfgTier.Description) ; null si aucun.") String fournisseurNom,
        @Schema(description = "Code de la catégorie de dépense (StlCategory).") String categorieCode,
        @Schema(description = "Libellé de la catégorie de dépense.") String categorieDescription,
        @Schema(description = "Date de début contractuelle (StartDateManual).") LocalDate dateDebut,
        @Schema(description = "Date de fin contractuelle (EndDateManual = début + délai).") LocalDate dateFin,
        @Schema(description = "Délai contractuel en jours (DelayManual).") Integer delaiJours,
        @Schema(description = "Lot multi-fournisseur (IsMultiSupplier).") boolean multiFournisseur,
        @Schema(description = "Montant marché HT : Σ bordereau définitif, ou forfait saisi si aucune ligne.")
        BigDecimal montantHT,
        @Schema(description = "Montant marché TVA (même règle que montantHT).") BigDecimal montantTVA,
        @Schema(description = "Montant marché TTC (même règle que montantHT).") BigDecimal montantTTC,
        @Schema(description = "Vrai si les montants viennent du bordereau, faux si du forfait saisi (*Manual).")
        boolean montantsDepuisBordereau,
        @Schema(description = "Taux de retenue de garantie du lot, en pourcentage 0–100.", example = "10")
        BigDecimal tauxRetenueGarantie,
        @Schema(description = "Nombre de décomptes du lot.") long nbDecomptes,
        @Schema(description = "Σ AmountHT des décomptes (montants stockés).") BigDecimal cumulDecomptesHT,
        @Schema(description = "Σ AmountTTC des décomptes (montants stockés).") BigDecimal cumulDecomptesTTC,
        @Schema(description = "Σ des travaux supplémentaires TTC (extraAmount).") BigDecimal cumulTravauxSupplementaires,
        @Schema(description = "Σ des retenues de garantie (WarrantyAmount).") BigDecimal cumulRetenueGarantie,
        @Schema(description = "cumulDecomptesTTC / montantTTC × 100, 2 décimales ; null si marché TTC nul. Peut dépasser 100.")
        BigDecimal avancementPct,
        @Schema(description = "Σ des nets à payer des factures de décompte (BuyDocument.AmountToPay).")
        BigDecimal totalFacture,
        @Schema(description = "Σ des règlements non annulés de ces factures.") BigDecimal totalRegle,
        @Schema(description = "totalFacture − totalRegle.") BigDecimal resteARegler) {
}
