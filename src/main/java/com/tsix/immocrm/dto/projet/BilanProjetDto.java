package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Bilan des projets demandés : arbre des catégories de dépenses × 8 colonnes, avec écart.")
public record BilanProjetDto(
        @Schema(description = "Les 8 colonnes, dans l'ordre d'affichage.") List<ColonneBilanDto> colonnes,
        @Schema(description = "Catégories racines ; une catégorie nulle avec tous ses descendants est masquée.")
        List<NoeudBilanDto> lignes,
        @Schema(description = "Code de la colonne 1 de l'écart, tel que reçu ; null si absent.") String ecart1,
        @Schema(description = "Code de la colonne 2 de l'écart, tel que reçu ; null si absent.") String ecart2,
        @Schema(description = "true : montants TTC ; false : HT.") boolean ttc) {
}
