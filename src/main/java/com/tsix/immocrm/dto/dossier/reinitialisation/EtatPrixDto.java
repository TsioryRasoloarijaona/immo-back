package com.tsix.immocrm.dto.dossier.reinitialisation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Superficie, TVA et prix d'un dossier, avant ou après réinitialisation.")
public record EtatPrixDto(
        @Schema(description = "Superficie de l'article (m²).")
        BigDecimal superficie,
        @Schema(description = "Superficie globale, sous-articles compris (m²).")
        BigDecimal superficieGlobale,
        @Schema(description = "Taux de TVA en ratio 0–1.")
        BigDecimal tauxTva,
        @Schema(description = "Prix du m² HT de l'article.")
        BigDecimal prixM2HT,
        @Schema(description = "Prix des sous-articles HT avant remise.")
        BigDecimal prixSousArticlesHT,
        @Schema(description = "Total HT avant remise.")
        BigDecimal totalHT,
        @Schema(description = "Montant de la remise HT (inchangé par la réinitialisation).")
        BigDecimal remiseMontantHT,
        @Schema(description = "Remise en pourcentage 0–100 (recalculée : Montant remise / Total HT).")
        BigDecimal remisePourcentage,
        @Schema(description = "Prix de vente HT après remise.")
        BigDecimal prixVenteHT,
        @Schema(description = "Prix de vente TTC après remise.")
        BigDecimal prixVenteTTC
) {
}
