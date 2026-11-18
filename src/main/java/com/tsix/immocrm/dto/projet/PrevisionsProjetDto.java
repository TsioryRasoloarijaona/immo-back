package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.dto.projet.PrevisionsLotDto.PointPrevisionDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Prévisions des lots d'un projet (OMRAN « Prévisions des lôts ») : totaux, graphe, lots, décaissements, avancements.")
public record PrevisionsProjetDto(
        @Schema(description = "Projet (PrjProject.PKey).") UUID projetId,
        @Schema(description = "Description du projet.") String projetDescription,
        @Schema(description = "Date du jour servant au « dû » (échéances strictement antérieures).") LocalDate aujourdhui,
        @Schema(description = "Bloc « Net à payer » : Prévision, Prévision ajustée, Réel.") TotauxNetAPayerDto totaux,
        @Schema(description = "Mois consécutifs du graphe, sommes des lots ; avancements pondérés par le HT prévisionnel.")
        List<PointPrevisionDto> mois,
        @Schema(description = "Une ligne par lot du périmètre (multi-fournisseurs compris).") List<LotPrevisionProjetDto> lots,
        @Schema(description = "Grilles « Décaissement » des trois onglets.") DecaissementsPrevisionDto decaissements,
        @Schema(description = "Grilles « Avancement » des trois onglets.") AvancementsPrevisionDto avancements
) {

    @Schema(description = "Totaux « Net à payer » des trois séries.")
    public record TotauxNetAPayerDto(
            @Schema(description = "Prévision (estimation de base simulée).") TotalNetAPayerDto prevision,
            @Schema(description = "Prévision ajustée : prévisions de paiement + réel.") TotalNetAPayerDto previsionAjustee,
            @Schema(description = "Réel : pièces payables ; total toujours null.") TotalNetAPayerDto reel
    ) {
    }

    @Schema(description = "Une ligne du bloc « Net à payer ».")
    public record TotalNetAPayerDto(
            @Schema(description = "Total ; null pour le Réel.") BigDecimal total,
            @Schema(description = "Dû à ce jour.") BigDecimal due,
            @Schema(description = "Reste ; pour le Réel, Σ des prévisions de paiement (code OMRAN).") BigDecimal reste,
            @Schema(description = "Avancement du projet au mois du jour (fraction 0–1).") BigDecimal avancement
    ) {
    }

    @Schema(description = "Lignes de décaissement des trois onglets, triées par date.")
    public record DecaissementsPrevisionDto(
            @Schema(description = "Estimation de base simulée des lots.") List<DecaissementPrevisionDto> prevision,
            @Schema(description = "Prévisions de paiement datées puis réel.") List<DecaissementPrevisionDto> previsionAjustee,
            @Schema(description = "Pièces payables hors avoirs, par échéance.") List<DecaissementPrevisionDto> reel
    ) {
    }

    @Schema(description = "Avancements du projet des trois onglets : seulement les mois où la valeur change.")
    public record AvancementsPrevisionDto(
            @Schema(description = "Avancement de l'estimation de base.") List<PointAvancementDto> prevision,
            @Schema(description = "Avancement des prévisions de paiement.") List<PointAvancementDto> previsionAjustee,
            @Schema(description = "Avancement des décomptes facturés.") List<PointAvancementDto> reel
    ) {
    }

    @Schema(description = "Ligne de décaissement d'un lot.")
    public record DecaissementPrevisionDto(
            @Schema(description = "Date de paiement prévue ou d'échéance.") LocalDate date,
            @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
            @Schema(description = "Description du lot.") String lot,
            @Schema(description = "Libellé : ligne simulée, description de la prévision ou code de la pièce.") String description,
            @Schema(description = "Montant net à payer.") BigDecimal montant,
            @Schema(description = "PREVISION ou REEL dans la Prévision ajustée ; null ailleurs.") String nature
    ) {
    }

    @Schema(description = "Avancement du projet à un mois.")
    public record PointAvancementDto(
            @Schema(description = "Premier jour du mois.") LocalDate mois,
            @Schema(description = "Σ poids × avancement des lots non multi-fournisseurs (fraction 0–1).") BigDecimal avancement
    ) {
    }
}
