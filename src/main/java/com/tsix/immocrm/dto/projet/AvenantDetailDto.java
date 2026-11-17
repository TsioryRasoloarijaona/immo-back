package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche avenant (PrjClause) avec ses lignes, ses retenues et les actions permises à l'appelant.")
public record AvenantDetailDto(
        @Schema(description = "PrjClause.PKey.") UUID id,
        @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Schema(description = "Ordre (Sorting).") Integer ordre,
        @Schema(description = "Code séquencé « Avenant ».", example = "26/00042") String code,
        @Schema(description = "« Avenant N° <code> du <date> », recalculée tant que l'avenant n'est pas facturé.") String description,
        @Schema(description = "Date (DocDate).") LocalDate date,
        @Schema(description = "Référence interne (DocRef).") String referenceInterne,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Fournisseur de l'avenant (CfgTier.Description), null sans fournisseur.") String fournisseur,
        @Schema(description = "Retenue de garantie.") RetenueAvenantDto garantie,
        @Schema(description = "Retenue du compte prorata.") RetenueAvenantDto prorata,
        @Schema(description = "Assurance DEC.") RetenueAvenantDto assuranceDEC,
        @Schema(description = "Assurance TRC.") RetenueAvenantDto assuranceTRC,
        @Schema(description = "Σ totaux HT des lignes (figé si facturé).") BigDecimal montantHT,
        @Schema(description = "TTC − HT (figé si facturé).") BigDecimal montantTVA,
        @Schema(description = "Σ totaux TTC des lignes (figé si facturé).") BigDecimal montantTTC,
        @Schema(description = "TTC − les 4 retenues (AmountToPay, figé si facturé).") BigDecimal netAPayer,
        @Schema(description = "Au moins une facture (INVOICE) rattachée à l'avenant.") boolean facture,
        @Schema(description = "Codes des factures séparés par « , » ; null sans facture.") String factureCode,
        @Schema(description = "Faux = lignes et retenues figées (avenant facturé) ; date, référence et notes restent modifiables.")
        boolean modifiable,
        @Schema(description = "DELETE permis : droit ClauseList.toolDelete et avenant non facturé.") boolean supprimable,
        @Schema(description = "Droit de saisir une retenue par montant (CanEditWorkingOutRetainByAmount).")
        boolean retenuesParMontantAutorisees,
        @Schema(description = "Lignes par ordre.") List<LigneAvenantDto> lignes
) {

    @Schema(description = "Ligne d'avenant (PrjClauseLine) avec ses montants calculés.")
    public record LigneAvenantDto(
            @Schema(description = "PrjClauseLine.Pkey.") UUID id,
            @Schema(description = "Ordre (Sorting).") Integer ordre,
            @Schema(description = "Code.") String code,
            @Schema(description = "Désignation (Description).") String designation,
            @Schema(description = "Unité.") String unite,
            @Schema(description = "Quantité.") BigDecimal quantite,
            @Schema(description = "Prix unitaire HT.") BigDecimal prixUnitaireHT,
            @Schema(description = "TVA en pourcentage 0–100.") BigDecimal tauxTVA,
            @Schema(description = "FODEC en pourcentage 0–100.") BigDecimal tauxFODEC,
            @Schema(description = "PU HT × (1 + FODEC) × (1 + TVA).") BigDecimal prixUnitaireTTC,
            @Schema(description = "Quantité × PU HT × (1 + FODEC).") BigDecimal totalHT,
            @Schema(description = "Quantité × PU TTC.") BigDecimal totalTTC,
            @Schema(description = "Catégorie (StlCategory.PKey).") UUID categorieId,
            @Schema(description = "Libellé de la catégorie.") String categorie,
            @Schema(description = "Ligne du bordereau définitif citée, null sinon.") UUID ligneBordereauId,
            @Schema(description = "Notes.") String notes
    ) {
    }
}
