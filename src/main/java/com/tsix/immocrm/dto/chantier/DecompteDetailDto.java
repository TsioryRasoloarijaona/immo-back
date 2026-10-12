package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche d'un décompte : montants stockés dans l'ordre OMRAN, facture, règlements, lignes, extras, retenues.")
public record DecompteDetailDto(
        @Schema(description = "Identifiant du décompte.") UUID id,
        @Schema(description = "Code du décompte.") String code,
        @Schema(description = "Numéro d'ordre dans le lot.") Integer numero,
        @Schema(description = "Date du décompte.") LocalDate dateDocument,
        @Schema(description = "Lot du décompte.") UUID lotId,
        @Schema(description = "Code du lot.") String lotCode,
        @Schema(description = "Désignation du lot.") String lotDescription,
        @Schema(description = "Projet du lot.") UUID projetId,
        @Schema(description = "Code du projet.") String projetCode,
        @Schema(description = "Fournisseur du décompte.") UUID fournisseurId,
        @Schema(description = "Nom du fournisseur.") String fournisseurNom,
        @Schema(description = "HT de la période (AmountHT).") BigDecimal montantHT,
        @Schema(description = "TTC de la période (AmountTTC).") BigDecimal montantTTC,
        @Schema(description = "Travaux supplémentaires TTC (extraAmount).") BigDecimal travauxSupplementaires,
        @Schema(description = "Retenue de garantie de la période (WarrantyAmount).") BigDecimal retenueGarantie,
        @Schema(description = "Net à payer (AmountToPay).") BigDecimal netAPayer,
        @Schema(description = "Cumul du lot net de RG, sans extras (AmountTTCAfterDeductions).")
        BigDecimal cumulNetRetenueGarantie,
        @Schema(description = "Dernier décompte du lot.") boolean dernier,
        @Schema(description = "Facture ; null si non facturé.") UUID factureId,
        @Schema(description = "Code de la facture.") String factureCode,
        @Schema(description = "Code du statut de la facture.") String factureStatutCode,
        @Schema(description = "Libellé du statut de la facture.") String factureStatutDescription,
        @Schema(description = "Règlements non annulés de la facture.") BigDecimal montantRegle,
        @Schema(description = "Net à payer de la facture − réglé.") BigDecimal resteARegler,
        @Schema(description = "TVA de la période (AmountVat).") BigDecimal montantTVA,
        @Schema(description = "Taux de RG du décompte (PrjWorkingOut.WarrantyRatio), pourcentage 0–100.")
        BigDecimal tauxRetenueGarantie,
        @Schema(description = "TTC − RG de la période (WorkingOutAmountTTC).") BigDecimal montantNetRetenueGarantie,
        @Schema(description = "Fluctuations à payer, incluses dans le net (FluctuationsToPay).") BigDecimal fluctuationsAPayer,
        @Schema(description = "Avance amortie (AdvanceAmount).") BigDecimal avanceAmortie,
        @Schema(description = "Avance sur décompte (WorkingOutAdvanceAmount).") BigDecimal avanceSurDecompte,
        @Schema(description = "Compte prorata (proportionAmount).") BigDecimal compteProrata,
        @Schema(description = "Assurance décennale (insuranceDECAmount).") BigDecimal assuranceDEC,
        @Schema(description = "Assurance tous risques chantier (insuranceTRCAmount).") BigDecimal assuranceTRC,
        @Schema(description = "Retenues non remboursables (nonRefundableRetainer).") BigDecimal retenuesNonRemboursables,
        @Schema(description = "Retenues remboursables (refundableRetainer).") BigDecimal retenuesRemboursables,
        @Schema(description = "Fluctuations (résidu d'arrondi en pratique).") BigDecimal fluctuations,
        @Schema(description = "Montant brut (amountToBePaid) : TTC, ou l'extra si le décompte n'a que des extras.")
        BigDecimal montantBrut,
        @Schema(description = "Σ AmountTTC des décomptes du lot de numéro ≤ celui-ci (seul montant calculé).")
        BigDecimal cumulTTC,
        @Schema(description = "Notes ; null si vides.") String notes,
        @Schema(description = "Date de création.") LocalDateTime dateCreation,
        @Schema(description = "Auteur de la création.") String creePar,
        @Schema(description = "Date de dernière modification.") LocalDateTime dateModification,
        @Schema(description = "Auteur de la dernière modification.") String modifiePar,
        @Schema(description = "Facture du décompte ; null si non facturé.") FactureDecompteDto facture,
        @Schema(description = "Règlements de la facture, annulés compris (annule = true).") List<ReglementFactureDto> reglements,
        @Schema(description = "Lignes ; vide pour un décompte saisi en travaux supplémentaires seuls.")
        List<LigneDecompteDto> lignes,
        @Schema(description = "Travaux supplémentaires.") List<ExtraDecompteDto> extras,
        @Schema(description = "Retenues forfaitaires.") List<RetenueDecompteDto> retenues) {
}
