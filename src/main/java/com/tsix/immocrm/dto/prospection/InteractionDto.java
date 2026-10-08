package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** Ligne {@code ComActionMessage} ; aussi servie par {@code GET /api/hotline/recherche}. */
@Schema(description = "Une interaction (appel, message, visite, note...) journalisée sur une "
        + "prospection ou une fiche.")
public record InteractionDto(

        UUID id,

        @Schema(description = "Date/heure de l'interaction.")
        LocalDateTime date,

        @Schema(example = "la cliente cherche 2 chambres et salon, rdv mercredi 10h")
        String note,

        @Schema(example = "07/09/2026 17:13 Khadija ELKORRI")
        String description,

        @Schema(description = "true si entrante, false si sortante.")
        Boolean entrante,

        UUID employeId,

        @Schema(example = "Khadija ELKORRI")
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

        LocalDateTime dateRdv,

        UUID categorieId,

        @Schema(example = "Prospection")
        String categorie,

        UUID trancheId,

        @Schema(description = "Code de la tranche. null si trancheId est null.", example = "CITEB2")
        String trancheCode,

        @Schema(description = "Libellé de la tranche. null si trancheId est null.")
        String trancheLibelle,

        @Schema(description = "Id du projet, déduit de la tranche. 96 %. null si trancheId est "
                + "null.")
        UUID projetId,

        @Schema(description = "Code du projet. null si projetId est null.", example = "CITEB")
        String projetCode,

        @Schema(description = "Libellé du projet. null si projetId est null.")
        String projetLibelle
) {
}
