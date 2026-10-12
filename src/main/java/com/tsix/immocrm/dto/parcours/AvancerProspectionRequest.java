package com.tsix.immocrm.dto.parcours;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Kanban : nouvelle étape et prochaine action obligatoire, écrites dans une seule transaction.")
public record AvancerProspectionRequest(
        @Schema(description = "Étape d'arrivée (ComProspectionKind.PKey).", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID etapeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ProchaineActionRequest prochaineAction
) {

    @Schema(description = "Prochaine action : l'interaction qui porte la relance suivante (ComActionMessage).")
    public record ProchaineActionRequest(
            @Schema(description = "Action (ComActionMessageType.PKey), ex. Téléphone Sortant.", requiredMode = Schema.RequiredMode.REQUIRED)
            UUID typeId,
            @Schema(description = "Résultat (ComActionMessageSubCategory.PKey) ; obligatoire sauf pour une Note.")
            UUID sousCategorieId,
            @Schema(description = "Échéance de la relance ; aujourd'hui ou plus tard.", requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDateTime date,
            @Schema(description = "Message de la relance (AlertMessage).", example = "Visite du 3 chambres témoin")
            String message
    ) {
    }
}
