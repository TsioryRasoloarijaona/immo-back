package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Bloc d'avance de l'onglet Suivi ; pour l'avance sur décompte, taux, estimations et marché sont null.")
public record BlocAvanceDto(
        @Schema(description = "Taux d'avance en pourcentage 0–100.") BigDecimal taux,
        @Schema(description = "Avance sur l'Est. de base (taux × forfait TTC).") BigDecimal estBase,
        @Schema(description = "Avance sur l'Est. confidentielle (taux × estimation TTC).") BigDecimal confidentielle,
        @Schema(description = "Avance du marché (taux × marché TTC ; avances des bons de commande si multi-fournisseur).")
        BigDecimal marche,
        @Schema(description = "Validée : Σ TTC des pièces d'avance du lot.") BigDecimal validee,
        @Schema(description = "Réglée : Σ des règlements de ces pièces.") BigDecimal reglee,
        @Schema(description = "Amortie dans les factures.") BigDecimal amortie,
        @Schema(description = "Reste à amortir = réglée − amortie, négatif conservé.") BigDecimal resteAAmortir
) {
}
