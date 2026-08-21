package com.tsix.immocrm.dto.referentiel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * One entry of the "Résultat" dropdown for {@code POST /api/prospections/{id}/interactions}
 * (see {@code GET /api/referentiels/resultats-interaction}) — backed by
 * {@code ComActionMessageSubCategory}.
 */
@Schema(description = "Un résultat possible pour une interaction (appel, message, visite...), "
        + "avec les indications nécessaires pour adapter le formulaire côté front sans avoir à "
        + "coder en dur les libellés métier.")
public record ResultatInteractionDto(

        UUID id,

        @Schema(description = "Libellé du résultat, ex: \"Rendez vous pris\", \"Hors Budget\".")
        String code,

        String description,

        @Schema(description = "true si ce résultat rend le champ \"Date du rendez-vous\" "
                + "obligatoire à la création de l'interaction (résultats \"Rendez vous pris\", "
                + "\"RDV reporté\" et \"Le prospect veut faire une visite du projet\") — "
                + "POST /api/prospections/{id}/interactions renvoie 400 si dateRdv est absente "
                + "dans ce cas, et crée une entrée exploitable par GET /api/calendrier/evenements.")
        boolean necessiteDateRdv,

        @Schema(description = "true si la donnée légataire indique qu'une relance (dateAlerte) "
                + "est recommandée pour ce résultat. Purement indicatif — jamais imposé côté "
                + "serveur.")
        boolean alerteRecommandee
) {
}
