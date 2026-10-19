package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Création ou modification d'une zone. En PATCH, absent ou null = inchangé ; le parent ne change pas.")
public record ZoneProjetEcritureDto(
        @Schema(description = "Code, unique parmi les zones du même parent du programme. Obligatoire.") String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        @Schema(description = "Zone parente (création seulement) ; absente = racine.") UUID parentId,
        @Schema(description = "Notes (1000 caractères au plus).") String notes
) {
}
