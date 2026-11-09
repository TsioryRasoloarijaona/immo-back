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
        @Schema(description = "TTC − RG de la période (WorkingOutAmountTTC).") BigDecimal montantNetRetenueGarantie,
        @Schema(description = "Avance amortie (AdvanceAmount).") BigDecimal avanceAmortie,
        @Schema(description = "Avance sur décompte (WorkingOutAdvanceAmount).") BigDecimal avanceSurDecompte,
        @Schema(description = "Compte prorata (proportionAmount).") BigDecimal compteProrata,
        @Schema(description = "Assurance décennale (insuranceDECAmount).") BigDecimal assuranceDEC,
        @Schema(description = "Assurance tous risques chantier (insuranceTRCAmount).") BigDecimal assuranceTRC,
        @Schema(description = "Retenues non remboursables (nonRefundableRetainer).") BigDecimal retenuesNonRemboursables,
        @Schema(description = "Retenues remboursables (refundableRetainer).") BigDecimal retenuesRemboursables,
        @Schema(description = "Fluctuations (Fluctuations).") BigDecimal fluctuations,
        @Schema(description = "Fluctuations à payer (FluctuationsToPay), comprises dans le net.") BigDecimal fluctuationsAPayer,
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
        @Schema(description = "Retenues (PrjWorkingOutRetainedLine), tri par ordre.") List<RetenueDecompteDto> retenues
) {
}
