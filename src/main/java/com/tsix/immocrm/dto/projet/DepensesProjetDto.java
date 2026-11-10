package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Synthèse des dépenses d'un projet visible : ses factures fournisseurs (INVOICE) seulement.")
public record DepensesProjetDto(
        @Schema(description = "Projet de chantier (PrjProject.PKey).") UUID projetId,
        @Schema(description = "Code du projet.") String projetCode,
        @Schema(description = "Description du projet.") String projetDescription,
        @Schema(description = "Nombre de factures du projet.") int nbFactures,
        @Schema(description = "Σ des montants TTC (AmountTTC).") BigDecimal montantTTC,
        @Schema(description = "Σ des montants à payer, chacun arrondi à 2 décimales.") BigDecimal montantAPayer,
        @Schema(description = "Σ des réglés des factures, règlements annulés exclus.") BigDecimal montantRegle,
        @Schema(description = "Σ à payer − Σ réglé (les factures trop réglées le diminuent).") BigDecimal resteARegler,
        @Schema(description = "Factures dont le reste dépasse 0,05.") int nbNonSoldees,
        @Schema(description = "Σ des restes des seules factures non soldées.") BigDecimal resteNonSoldees,
        @Schema(description = "Factures trop réglées (reste < −0,05, pièce à payer positive).") int nbTropReglees,
        @Schema(description = "Factures liées à un décompte (PrjWorkingOut.BuyDocumentId).") int nbFacturesDecompte,
        @Schema(description = "Une ligne par lot ayant des factures, tri par code, puis la ligne « Hors lot » en dernier.")
        List<DepensesLotDto> lots) {
}
