package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Projet de travaux (PrjProject) avec les totaux de ses lots, recalculés comme ceux de chaque lot.")
public record ProjetChantierListItemDto(
        @Schema(description = "Identifiant du projet (PrjProject.PKey).") UUID id,
        @Schema(description = "Code du projet.", example = "ARS-PRJ") String code,
        @Schema(description = "Désignation du projet.") String description,
        @Schema(description = "Code de la société (CfgCompany) ; null si absente.") String societeCode,
        @Schema(description = "Libellé de la société.") String societeDescription,
        @Schema(description = "Code du statut du projet (StrStatus).") String statutCode,
        @Schema(description = "Libellé du statut du projet.") String statutDescription,
        @Schema(description = "Nombre de lots (PrjMarket) du projet.") long nbLots,
        @Schema(description = "Σ des montants marché HT des lots (même calcul que LotListItemDto.montantHT).")
        BigDecimal totalMarchesHT,
        @Schema(description = "Σ des montants marché TTC des lots.") BigDecimal totalMarchesTTC,
        @Schema(description = "Nombre de décomptes de tous les lots.") long nbDecomptes,
        @Schema(description = "Σ des montants TTC stockés des décomptes (AmountTTC).") BigDecimal totalDecomptesTTC,
        @Schema(description = "Σ des nets à payer des factures de décompte (BuyDocument.AmountToPay).")
        BigDecimal totalFacture,
        @Schema(description = "Σ des règlements non annulés de ces factures.") BigDecimal totalRegle,
        @Schema(description = "totalFacture − totalRegle.") BigDecimal totalResteARegler,
        @Schema(description = "Vrai si le projet n'a aucun lot et un code « AAAA-… » : centre de coûts annuel.")
        boolean estCentreDeCouts) {
}
