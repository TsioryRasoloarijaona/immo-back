package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Body for {@code POST /api/prospections/{id}/interactions}. {@code description} is required;
 * {@code entrante} defaults to {@code true} (incoming) when omitted.
 */
@Schema(description = "Corps de création d'une interaction (appel, message, visite...) sur une "
        + "prospection ou une fiche.")
public record CreerInteractionRequest(

        @Schema(description = "Texte libre décrivant l'interaction.", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "true si l'interaction est entrante (l'appelant/le prospect a "
                + "initié le contact), false si sortante. Par défaut true si omis.")
        Boolean entrante,

        @Schema(description = "Id de l'\"Action\" (nature de l'interaction : Instruction, Mail "
                + "entrant/Sortant, Message entrant/sortant, Note, Téléphone entrant/Sortant, "
                + "Visite, WEB). Référentiel : GET /api/referentiels/types-contact-prospection. "
                + "Optionnel.")
        UUID actionTypeId,

        @Schema(description = "Id du \"Résultat\" de l'interaction (ex: \"Rendez vous pris\", "
                + "\"Hors Budget\", \"Client injoignable ou Boite vocale\", \"Le prospect veut "
                + "faire une visite du projet\"...). Référentiel : "
                + "GET /api/referentiels/resultats-interaction. Optionnel — mais si le résultat "
                + "choisi a necessiteDateRdv=true (\"Rendez vous pris\", \"RDV reporté\" ou "
                + "\"Le prospect veut faire une visite du projet\"), dateRdv devient obligatoire.")
        UUID resultatId,

        @Schema(description = "Date jusqu'à laquelle une relance sur cette interaction reste "
                + "pertinente (alimente le rappel/l'alerte de suivi). Recommandé quand le "
                + "référentiel resultat indique alerteRecommandee=true, mais jamais imposé "
                + "côté serveur. Optionnel.")
        LocalDateTime dateAlerte,

        @Schema(description = "Date/heure du rendez-vous ou de la visite planifiée avec le "
                + "prospect/client. Obligatoire si le résultat choisi est \"Rendez vous pris\", "
                + "\"RDV reporté\" ou \"Le prospect veut faire une visite du projet\" (voir "
                + "resultatId) ; ignoré sinon. Crée une entrée calendrier ({@code AdmEvent}) "
                + "rattachée à cette interaction, restituée par GET /api/calendrier/evenements "
                + "pour affichage dans un calendrier côté front.")
        LocalDateTime dateRdv
) {
}
