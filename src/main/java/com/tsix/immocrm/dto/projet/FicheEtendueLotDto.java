package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Champs de la fiche lot OMRAN : en-tête, onglets Dates, Options, Résiliation et Autres (à plat dans LotDetailDto).")
public record FicheEtendueLotDto(
        @Schema(description = "Type de lot (PrjMarketType.PKey), null si absent.") UUID typeId,
        @Schema(description = "Devise (StlCurrency.PKey), null si absente.") UUID deviseId,
        @Schema(description = "Code de la devise.", example = "MAD") String deviseCode,
        @Schema(description = "Catégorie (StlCategory.PKey), null si absente.") UUID categorieId,
        @Schema(description = "Est. de base : date de début (StartDateManual).") LocalDate dateDebutBase,
        @Schema(description = "Est. de base : durée en jours (DelayManual).") Integer delaiBaseJours,
        @Schema(description = "Est. de base : date de fin (EndDateManual).") LocalDate dateFinBase,
        @Schema(description = "Date de report de l'appel d'offres (TendersResortDate).") LocalDate dateReportAppelOffres,
        @Schema(description = "Date du rapport de dépouillement (TendersReportDate).") LocalDate dateRapportDepouillement,
        @Schema(description = "Date d'achèvement des travaux (WorkCompletionDate).") LocalDate dateAchevementTravaux,
        @Schema(description = "Date de recollement (ReattachmentDate).") LocalDate dateRecollement,
        @Schema(description = "Variation dans la masse des travaux en pourcentage 0–100 (MassChangeRatio).")
        BigDecimal variationMassePct,
        @Schema(description = "Taux de remboursement de l'approvisionnement en pourcentage 0–100 (StockPileRatio).")
        BigDecimal tauxRemboursementApproPct,
        @Schema(description = "Total des points (TotalPoints).") Integer totalPoints,
        @Schema(description = "Type de paiement (StlSettlementType.PKey), null si absent.") UUID typePaiementId,
        @Schema(description = "Libellé du type de paiement, null si absent.") String typePaiementLibelle,
        @Schema(description = "Remise appliquée aux travaux supplémentaires (ApplyDiscountOnAdditionalWork).")
        boolean remiseSurTravauxSupp,
        @Schema(description = "Retenue à la source sur travaux supplémentaires (ApplyWithholdingTaxOnAdditionalWork).")
        boolean retenueSurTravauxSupp,
        @Schema(description = "Décomptes saisis sur les quantités exécutées (ExecutedQuantityBasedInsert).")
        boolean saisieSurQuantites,
        @Schema(description = "Avancement modifiable par ligne de décompte (CanEditWorkingOutLineByAchivement).")
        boolean avancementParLigne,
        @Schema(description = "Date de résiliation (CancellationDate).") LocalDate dateResiliation,
        @Schema(description = "Cause de la résiliation (CancellationCause), chaîne vide possible.") String causeResiliation,
        @Schema(description = "Description détaillée (DescriptionLong).") String descriptionDetaillee
) {
}
