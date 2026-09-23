package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** Une prospection existante au même contact qu'une pré-prospection qu'on vient de créer. */
@Schema(description = "Prospection ou pré-prospection existante portant le même contact (nom, "
        + "téléphone, email) que celle qui vient d'être créée.")
public record DoublonPreProspectionDto(
        @Schema(description = "Identifiant (ComProspection.PKey) — ouvrable par "
                + "`GET /api/prospections/{id}`.")
        UUID id,
        @Schema(description = "Code du document, p.ex. `26/05940`. Peut être atypique (`50`) sur les "
                + "leads injectés par n8n.", example = "26/05940")
        String code,
        @Schema(description = "Date de création (`UserCreateDateTime`), null sur certaines lignes "
                + "anciennes ou importées.")
        LocalDateTime dateCreation
) {
}
