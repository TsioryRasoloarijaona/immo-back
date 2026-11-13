package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne d'un bordereau envoyée à l'enregistrement ; le PU TTC est recalculé par le serveur.")
public record LigneSaisieDto(
        @Schema(description = "Id de la ligne : existante = modifiée, inconnue = insérée (UUID généré par le front).") UUID id,
        @Schema(description = "Groupe de la ligne, du même envoi ; null sans groupe.") UUID groupeId,
        @Schema(description = "Ordre (Sorting), obligatoire.") Integer ordre,
        @Schema(description = "Code (255 caractères au plus) ; unique parmi les lignes de l'estimatif.") String code,
        @Schema(description = "Désignation, obligatoire (255 caractères au plus).") String designation,
        @Schema(description = "Unité, texte libre (50 caractères au plus).") String unite,
        @Schema(description = "Quantité, obligatoire, ≥ 0 ; arrondie à 6 décimales.") BigDecimal quantite,
        @Schema(description = "Prix unitaire HT, obligatoire, ≥ 0.") BigDecimal prixUnitaireHT,
        @Schema(description = "Taux de TVA en pourcentage, obligatoire, de 0 à 100.", example = "20") BigDecimal tauxTVA,
        @Schema(description = "Taux de FODEC en pourcentage ; null = 0.", example = "0") BigDecimal tauxFODEC,
        @Schema(description = "Catégorie de dépense (StlCategory), obligatoire et existante.") UUID categorieId,
        @Schema(description = "Pondération (WeightingRatio), stockée telle quelle ; null accepté.") BigDecimal ponderation,
        @Schema(description = "Notes (1 000 caractères au plus).") String notes
) {
}
