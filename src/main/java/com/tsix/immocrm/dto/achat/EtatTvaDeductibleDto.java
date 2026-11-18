package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "État de la TVA déductible : lignes des factures de la période, cumul par mois et totaux hors annulées.")
public record EtatTvaDeductibleDto(
        @Schema(description = "Une ligne par ligne de facture, une seule pour une facture sans ligne ; par date, code, ordre.")
        List<LigneTvaDeductibleDto> lignes,
        @Schema(description = "Un élément par mois ayant au moins une facture non annulée, par mois croissant.")
        List<MoisTvaDeductibleDto> parMois,
        TotauxTvaDeductibleDto totaux) {

    @Schema(description = "Ligne de l'état de la TVA déductible.")
    public record LigneTvaDeductibleDto(
            @Schema(description = "Facture (BuyDocument.PKey).") UUID factureId,
            @Schema(description = "Code de la facture.") String factureCode,
            @Schema(description = "Date de la facture (DocDate).") LocalDate dateFacture,
            @Schema(description = "Libellé du statut de la facture, null sans statut.") String statut,
            @Schema(description = "Vrai si le statut est de type Annulation : ligne listée, exclue des totaux.") boolean annulee,
            @Schema(description = "Nom du fournisseur (CfgTier.Description), null sans fournisseur.") String fournisseur,
            @Schema(description = "Description du projet, null sans projet.") String projet,
            @Schema(description = "Description de la société.") String societe,
            @Schema(description = "Description de la ligne ; celle de la facture si sansLignes.") String description,
            @Schema(description = "Quantité de la ligne, null si sansLignes.") BigDecimal quantite,
            @Schema(description = "Prix unitaire HT de la ligne, null si sansLignes.") BigDecimal prixUnitaireHT,
            @Schema(description = "Remise en pourcentage (DiscountRatio × 100), null si sansLignes.") BigDecimal remisePct,
            @Schema(description = "Montant HT exact, non arrondi.") BigDecimal montantHT,
            @Schema(description = "Taux de TVA en pourcentage (VatRatio × 100).") BigDecimal tauxTvaPct,
            @Schema(description = "Montant TVA = montant HT × taux, exact, non arrondi.") BigDecimal montantTva,
            @Schema(description = "Vrai pour une facture sans ligne : montants lus sur l'en-tête (AmountHT, AmountVat).")
            boolean sansLignes) {
    }

    @Schema(description = "Cumul d'un mois, factures annulées exclues.")
    public record MoisTvaDeductibleDto(
            @Schema(description = "Premier jour du mois.", example = "2026-01-01") LocalDate mois,
            @Schema(description = "Nombre de factures distinctes.") long factures,
            @Schema(description = "Σ des montants HT.") BigDecimal montantHT,
            @Schema(description = "Σ des montants TVA.") BigDecimal montantTva) {
    }

    @Schema(description = "Totaux de l'état, factures annulées exclues.")
    public record TotauxTvaDeductibleDto(
            @Schema(description = "Nombre de factures distinctes.") long factures,
            @Schema(description = "Σ des montants HT.") BigDecimal montantHT,
            @Schema(description = "Σ des montants TVA (TVA déductible).") BigDecimal montantTva,
            @Schema(description = "Σ TVA des factures dont le statut est validé en comptabilité.") BigDecimal montantTvaValidee,
            @Schema(description = "Σ TVA des autres factures (en attente de validation).") BigDecimal montantTvaEnAttente) {
    }
}
