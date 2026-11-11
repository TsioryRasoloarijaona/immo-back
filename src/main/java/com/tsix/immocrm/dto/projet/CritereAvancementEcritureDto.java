package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Saisie d'un critère d'avancement (ajout ou remplacement).")
public record CritereAvancementEcritureDto(
        @Schema(description = "O.D.T. ; absent à l'ajout = nombre de critères du projet + 1, absent en modification = inchangé.")
        Integer ordre,
        @Schema(description = "Description, obligatoire, 255 caractères au plus.", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,
        @Schema(description = "Catégorie de dépense (StlCategory non crédit), obligatoire.",
                requiredMode = Schema.RequiredMode.REQUIRED) UUID categorieId
) {
}
