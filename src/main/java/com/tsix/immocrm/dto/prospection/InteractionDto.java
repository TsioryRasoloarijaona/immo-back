package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shape for entries in {@code GET /api/prospections/{id}/interactions} (journal, backed by
 * {@code ComActionMessage}) and in the interaction lists embedded by
 * {@code GET /api/hotline/recherche}.
 */
@Schema(description = "Une interaction (appel, message, visite, note...) journalisée sur une "
        + "prospection ou une fiche.")
public record InteractionDto(

        UUID id,

        @Schema(description = "Date/heure de l'interaction.")
        LocalDateTime date,

        @Schema(description = "**Le texte saisi par le commercial — c'est ce qu'il faut "
                + "afficher dans le journal.** Stocké dans `ComActionMessage.AlertMessage`, qui "
                + "malgré son nom ne relève pas des relances : 6 502 interactions portent une "
                + "note sans aucune date d'alerte. `null` quand rien n'a été saisi (3,4 % des "
                + "lignes réelles).",
                example = "la cliente cherche 2 chambres et salon, rdv mercredi 10h")
        String note,

        @Schema(description = "**Libellé généré, pas un contenu.** Vaut `JJ/MM/AAAA HH:MM "
                + "<auteur>` sur 100 % des lignes réelles (46 546 sur 46 546, zéro exception), "
                + "c'est-à-dire exactement `date` + `employeNom`, déjà présents ci-dessus. "
                + "Conservé pour ne rien casser, mais **l'afficher revient à répéter trois fois "
                + "la même information** — utilisez `note`.",
                example = "07/09/2026 17:13 Khadija ELKORRI")
        String description,

        @Schema(description = "true si entrante, false si sortante.")
        Boolean entrante,

        UUID employeId,
        String employeNom,

        @Schema(description = "Id de l'\"Action\" (nature de l'interaction). null si non "
                + "renseignée.")
        UUID actionTypeId,

        @Schema(description = "Libellé de l'\"Action\" (Instruction, Visite, Téléphone "
                + "entrant...). null si non renseignée.")
        String actionType,

        @Schema(description = "Id du \"Résultat\" de l'interaction. null si non renseigné.")
        UUID resultatId,

        @Schema(description = "Libellé du \"Résultat\" (Rendez vous pris, Hors Budget...). null "
                + "si non renseigné.")
        String resultat,

        @Schema(description = "Date de fin de validité de la relance/alerte, si renseignée. "
                + "null sinon.")
        LocalDateTime dateAlerte,

        @Schema(description = "Date du rendez-vous, présente uniquement quand resultat est "
                + "\"Rendez vous pris\" ou \"RDV reporté\" et qu'une date a été fournie à la "
                + "création. null sinon.")
        LocalDateTime dateRdv
) {
}
