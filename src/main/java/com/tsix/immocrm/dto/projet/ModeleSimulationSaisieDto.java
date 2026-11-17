package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche d'un modèle de simulation lot, envoyée ou proposée depuis les points de base d'un lot.")
public record ModeleSimulationSaisieDto(
        @Schema(description = "Code, obligatoire et unique.") String code,
        @Schema(description = "Description, obligatoire.") String description,
        @Schema(description = "Catégorie de dépense (StlCategory.PKey) ; null = tous les lots.") UUID categorieId,
        @Schema(description = "Lignes : au moins deux, temps maximal 1, somme des paiements entre 0,99 et 1,01.")
        List<LigneModeleDto> lignes
) {
}
