package com.tsix.immocrm.dto.calendrier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record EvenementCalendrierDto(

        @Schema(description = "Id de l'interaction (ComActionMessage) à l'origine de l'événement.")
        UUID interactionId,

        @Schema(description = "Sujet de l'événement — reprend la description de l'interaction.")
        String sujet,

        @Schema(description = "Date/heure de début de l'événement (la \"Date du rendez-vous\" "
                + "saisie sur l'interaction).")
        LocalDateTime dateDebut,

        @Schema(description = "Date/heure de fin de l'événement, si renseignée. Null la plupart "
                + "du temps — seule dateDebut est capturée aujourd'hui.")
        LocalDateTime dateFin,

        @Schema(description = "Lieu de l'événement, si renseigné.")
        String lieu,

        @Schema(description = "Id du \"Résultat\" (ComActionMessageSubCategory) à l'origine de "
                + "l'événement.")
        UUID resultatId,

        String resultatCode,

        String resultatDescription,

        UUID prospectionId,

        String prospectionCode,

        @Schema(description = "Id du prospect/client concerné, si connu.")
        UUID prospectId,

        String prospectNom,

        @Schema(description = "Id du commercial ayant loggé l'interaction, si connu.")
        UUID commercialId,

        String commercialNom
) {
}
