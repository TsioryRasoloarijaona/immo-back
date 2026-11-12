package com.tsix.immocrm.dto.projet;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Fiche d'un décompte : la ligne de liste, le détail des montants stockés, la facture et les lignes.")
public record DecompteDetailDto(
        @JsonUnwrapped DecompteListItemDto decompte,
        @Schema(description = "TVA de la période (AmountVat).") BigDecimal montantTVA,
        @Schema(description = "Taux de RG du décompte en pourcentage (WarrantyRatio × 100).") BigDecimal tauxRetenueGarantie,
        @Schema(description = "« Travaux du mois » : TTC − montants à déduire (WorkingOutAmountTTC).") BigDecimal montantNetRetenueGarantie,
        @Schema(description = "Avance amortie (AdvanceAmount).") BigDecimal avanceAmortie,
        @Schema(description = "Avance sur décompte (WorkingOutAdvanceAmount).") BigDecimal avanceSurDecompte,
        @Schema(description = "Compte prorata (proportionAmount).") BigDecimal compteProrata,
        @Schema(description = "Assurance décennale (insuranceDECAmount).") BigDecimal assuranceDEC,
        @Schema(description = "Assurance tous risques chantier (insuranceTRCAmount).") BigDecimal assuranceTRC,
        @Schema(description = "Retenues non remboursables (nonRefundableRetainer).") BigDecimal retenuesNonRemboursables,
        @Schema(description = "Retenues remboursables (refundableRetainer).") BigDecimal retenuesRemboursables,
        @Schema(description = "Fluctuations, nouvelle valeur (Fluctuations).") BigDecimal fluctuations,
        @Schema(description = "Montant brut (amountToBePaid).") BigDecimal montantBrut,
        @Schema(description = "Σ AmountTTC des décomptes du lot de numéro ≤ celui-ci.") BigDecimal cumulTTC,
        @Schema(description = "Création (UserCreateDateTime).") LocalDateTime dateCreation,
        @Schema(description = "Auteur de la création.") String creePar,
        @Schema(description = "Dernière modification (UserUpdateDateTime).") LocalDateTime dateModification,
        @Schema(description = "Auteur de la dernière modification.") String modifiePar,
        @Schema(description = "Facture du décompte, null si non facturé.") FactureDecompteDto facture,
        @Schema(description = "Règlements de la facture, annulés compris (annule = true), tri par date.")
        List<ReglementFactureDto> reglements,
        @Schema(description = "Lignes du décompte, tri par ordre ; vide pour un décompte « extras seuls ».")
        List<LigneDecompteDto> lignes,
        @Schema(description = "Travaux supplémentaires (PrjWorkingOutLineExtra), tri par ordre.") List<ExtraDecompteDto> extras,
        @Schema(description = "Retenues (PrjWorkingOutRetainedLine), tri par ordre.") List<RetenueDecompteDto> retenues,
        @Schema(description = "Référence interne (DocRef).") String referenceInterne,
        @Schema(description = "Avance « basée sur le montant » (AdvanceBasedOnAmount).") boolean avanceSurMontant,
        @Schema(description = "Retenue de garantie « basée sur le montant » (WarrantyBasedOnAmount).") boolean retenueGarantieSurMontant,
        @Schema(description = "Assurance décennale « basée sur le montant » (DECBasedOnAmount).") boolean assuranceDECSurMontant,
        @Schema(description = "Assurance TRC « basée sur le montant » (TRCBasedOnAmount).") boolean assuranceTRCSurMontant,
        @Schema(description = "Compte prorata « basé sur le montant » (ProrataBasedOnAmount).") boolean compteProrataSurMontant,
        @Schema(description = "Taux d'avance en pourcentage (AdvanceRatio × 100).") BigDecimal tauxAvance,
        @Schema(description = "Taux d'assurance décennale en pourcentage (insuranceDECRatio × 100).") BigDecimal tauxAssuranceDEC,
        @Schema(description = "Taux d'assurance TRC en pourcentage (insuranceTRCRatio × 100).") BigDecimal tauxAssuranceTRC,
        @Schema(description = "Taux du compte prorata en pourcentage (proportionRatio × 100).") BigDecimal tauxCompteProrata,
        @Schema(description = "Remboursement du compte prorata (ProrataRefundAmount).") BigDecimal remboursementCompteProrata,
        @Schema(description = "« Cumul précédent » : Σ AmountTTC des décomptes du lot de numéro inférieur.") BigDecimal cumulPrecedentTTC,
        @Schema(description = "Taux de remboursement de l'approvisionnement en pourcentage (StockPileRatio × 100).")
        BigDecimal tauxApprovisionnement,
        @Schema(description = "Approvisionnement, ancienne valeur TTC (StockAmountOld).") BigDecimal approvisionnementAncien,
        @Schema(description = "Approvisionnement, nouvelle valeur TTC (StockAmount).") BigDecimal approvisionnementNouveau,
        @Schema(description = "Fluctuations, ancienne valeur (FluctuationsOld).") BigDecimal fluctuationsAnciennes,
        @Schema(description = "Reste des avances du lot à la création (advanceRest).") BigDecimal resteAvance,
        @Schema(description = "Reste des avances sur décompte à la création (WorkingOutAdvanceAmountRestAtCreation).")
        BigDecimal resteAvanceSurDecompte,
        @Schema(description = "Régularisation (AmountRegul).") BigDecimal regularisation,
        @Schema(description = "Lignes d'approvisionnement (PrjWorkingOutStockLine), tri par ordre ; vide si aucune.")
        List<LigneApprovisionnementDto> approvisionnements,
        @Schema(description = "Vrai si l'appelant a le droit A_0152_13 et que le décompte n'est pas verrouillé (Enregistrer, Recharger).")
        boolean peutModifier,
        @Schema(description = "Vrai si l'appelant a A_0152_15 et A_0152_13 et que le décompte n'est pas verrouillé.")
        boolean peutRecalculer,
        @Schema(description = "Comme peutModifier, et le décompte n'est pas déjà le dernier décompte du lot (FinalWorkingOut).")
        boolean peutDefinirDernier
) {
}
