package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

// Aucune contrainte Bean Validation : le droit du genre est contrôlé avant le corps (403 avant 400).
@Schema(description = "Création d'un lot (fiche minimale) ; le code vient de la séquence OMRAN 'Market'.")
public record CreerLotRequest(
        @Schema(description = "Genre du lot.", requiredMode = REQUIRED,
                allowableValues = {"ENTREPRENEURS", "FOURNISSEURS", "ADMINISTRATION", "MAITRES_OEUVRE", "CONCESSIONNAIRES"})
        String genre,
        @Schema(description = "Désignation du lot.", requiredMode = REQUIRED, maxLength = 255) String description,
        @Schema(description = "Projet (PrjProject.PKey), visible de l'appelant.", requiredMode = REQUIRED) UUID projetId,
        @Schema(description = "Catégorie (StlCategory.PKey).", requiredMode = REQUIRED) UUID categorieId,
        @Schema(description = "Type de lot (PrjMarketType.PKey).", requiredMode = REQUIRED) UUID typeId,
        @Schema(description = "Fournisseur (CfgTier.PKey), facultatif.") UUID fournisseurId,
        @Schema(description = "Devise (StlCurrency.PKey), facultative.") UUID deviseId
) {
}
