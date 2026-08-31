package com.tsix.immocrm.dto.calendrier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une entrée de calendrier ({@code AdmEvent}) créée automatiquement lors de l'ajout d'une
 * interaction dont le "Résultat" est un rendez-vous ou une visite planifiée (voir
 * {@code POST /api/prospections/{id}/interactions}). Restituée par
 * {@code GET /api/calendrier/evenements} pour affichage dans un calendrier côté front.
 */
@Schema(description = "Un événement planifié (rendez-vous ou visite) à afficher dans le "
        + "calendrier du front. Une entrée par interaction ayant fixé une date de "
        + "rendez-vous/visite.")
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

        @Schema(description = "Code du résultat — permet au front de distinguer un rendez-vous "
                + "(\"Rendez vous pris\", \"RDV reporté\") d'une visite "
                + "(\"Le prospect veut faire une visite du projet\") sans coder de libellé en dur.")
        String resultatCode,

        String resultatDescription,

        @Schema(description = "Id de la prospection sur laquelle l'interaction a été loggée. "
                + "Null si l'interaction est rattachée directement à une fiche (hotline).")
        UUID prospectionId,

        String prospectionCode,

        @Schema(description = "Id du prospect/client concerné, si connu.")
        UUID prospectId,

        @Schema(description = "Nom affichable du prospect/client (raison sociale ou prénom nom).")
        String prospectNom,

        @Schema(description = "Id du commercial ayant loggé l'interaction, si connu.")
        UUID commercialId,

        String commercialNom
) {
}
