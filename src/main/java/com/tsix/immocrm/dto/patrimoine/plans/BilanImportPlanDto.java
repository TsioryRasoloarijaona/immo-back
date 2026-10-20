package com.tsix.immocrm.dto.patrimoine.plans;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Bilan d'un import SVG d'étage (ou de son aperçu).")
public record BilanImportPlanDto(
        @Schema(description = "Vrai si c'est un aperçu : rien n'a été écrit.") boolean apercu,
        @Schema(description = "PKey du plan créé ou mis à jour ; null pour l'aperçu d'un plan à créer.") UUID planId,
        @Schema(description = "Vrai si le plan de l'étage n'existait pas (création), faux s'il est mis à jour.") boolean planCree,
        @Schema(description = "Une ligne par `<path>` du fichier, dans l'ordre du fichier.") List<LigneBilanTraceDto> lignes
) {
}
