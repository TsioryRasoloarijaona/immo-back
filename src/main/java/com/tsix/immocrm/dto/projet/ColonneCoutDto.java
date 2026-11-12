package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Colonne « Coût prévisionnel » ou « Coût réel » de l'onglet Général (calculée côté serveur).")
public record ColonneCoutDto(
        @Schema(description = "Bordereau (lignes du marché, ou facturées pour un lot terminé / le coût réel).") TrioDto bordereau,
        @Schema(description = "Approvisionnements (StockAmountToPay).") TrioDto approvisionnement,
        @Schema(description = "Dépassements de quantité.") TrioDto depassements,
        @Schema(description = "Travaux supplémentaires.") TrioDto travauxSupplementaires,
        @Schema(description = "Fluctuations (révision des prix).") TrioDto fluctuations,
        @Schema(description = "Avenants (PrjClause), facturés pour un lot terminé / le coût réel.") TrioDto avenants,
        @Schema(description = "Coût total, avenants compris (= coutPrevisionnelHT/TTC de la liste) ; coût réel : facturé moins avoirs.")
        TrioDto total,
        @Schema(description = "Retenues non remboursables (compte prorata, assurances, retenues forfaitaires, pénalités).")
        BigDecimal retenueNonRemboursable,
        @Schema(description = "Remboursement du compte prorata.") BigDecimal remboursement,
        @Schema(description = "Régularisations et droits de timbre des pièces.") BigDecimal regulation,
        @Schema(description = "Net à payer = total TTC + remboursement + régulation − retenues non remboursables.")
        BigDecimal netAPayer,
        @Schema(description = "Réglé : Σ des lignes de règlement des pièces du lot (règlements annulés compris).") BigDecimal regle,
        @Schema(description = "Reste = net à payer − réglé.") BigDecimal reste
) {
}
