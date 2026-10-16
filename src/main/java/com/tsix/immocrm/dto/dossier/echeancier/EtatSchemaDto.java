package com.tsix.immocrm.dto.dossier.echeancier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "État du schéma de financement du dossier, calculé à la lecture depuis l'échéancier.")
public record EtatSchemaDto(
        @Schema(description = "Prix de vente après remise (`ComFolder.PriceAfterDiscount`).")
        BigDecimal prixApresRemise,

        @Schema(description = "Σ des lignes de catégorie « Avance », signées (une catégorie au débit se retranche).")
        BigDecimal avance,

        @Schema(description = "Σ des lignes de catégorie « Crédit », signées.")
        BigDecimal credit,

        @Schema(description = "Σ des lignes de catégorie « Autofinancement », signées.")
        BigDecimal autofinancement,

        @Schema(description = "Prix après remise − (avance + crédit + autofinancement), au centime ; 0 = schéma équilibré.")
        BigDecimal ecart,

        @Schema(description = "Autofinancement restant à régler.")
        BigDecimal autofinancementReste,

        @Schema(description = "Montant proposé et plafond de la périodicité : l'autofinancement restant, sinon prix − avance − crédit.")
        BigDecimal montantInitialPeriodicite
) {
}
