package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Catégorie de dépense du Bilan projet ; un parent cumule sa catégorie et ses descendants.")
public record NoeudBilanDto(
        @Schema(description = "Catégorie (StlCategory.PKey).") UUID categorieId,
        @Schema(description = "Libellé (StlCategory.Description).") String libelle,
        @Schema(description = "Profondeur, 0 pour les racines.") int niveau,
        @Schema(description = "Montant par code de colonne (les 8 codes), HT ou TTC selon `ttc`.")
        Map<String, BigDecimal> valeurs,
        @Schema(description = "colonne1 − colonne2 ; 0 si l'une des deux manque.") BigDecimal ecart,
        @Schema(description = "Sous-catégories non nulles, tri Sorting puis Description.") List<NoeudBilanDto> enfants) {
}
