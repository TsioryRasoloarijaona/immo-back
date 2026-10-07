package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** Calculé par la vue légataire {@code ComSaleProspectionView}. */
public record ActiviteProspectionDto(

        @Schema(example = "1688")
        Integer joursInactivite,

        @Schema(description = "Nombre d'interactions journalisées sur la prospection. 97 %.",
                example = "12")
        Integer nbInteractions,

        @Schema(description = "Date de la dernière action, tous types confondus. 97 %.")
        LocalDateTime derniereActionDate,

        @Schema(description = "Date de la dernière communication avec le prospect. 90 %.")
        LocalDateTime derniereCommunicationDate,

        @Schema(description = "Résultat de la dernière communication. `null` si aucune — la vue "
                + "rend une chaîne vide, normalisée en `null` ici. 90 %.",
                example = "Prospect n'est plus intéressé par un appartement")
        String derniereCommunicationResultat,

        @Schema(description = "Texte de la dernière communication. Même normalisation du vide en "
                + "`null`. 90 %.",
                example = "client voulait juste des informations")
        String derniereCommunicationMessage,

        @Schema(description = "Date de la relance en cours. `null` si aucune relance active — "
                + "c'est le cas de 75 % des prospections.")
        LocalDateTime alerteActiveDate,

        @Schema(description = "Motif de la relance en cours. `null` si aucune. 25 %.")
        String alerteActiveRaison,

        @Schema(description = "Nombre de visites effectuées. 20 %, ce qui correspond aux "
                + "prospections ayant atteint l'étape visite.",
                example = "2")
        Integer nbVisites,

        @Schema(description = "Date de la première visite. `null` si aucune visite.")
        LocalDateTime premiereVisiteDate,

        @Schema(description = "Date de la dernière visite. `null` si aucune visite.")
        LocalDateTime derniereVisiteDate,

        @Schema(description = "Identifiant du projet. Renseigné sur 99 % des prospections de "
                + "vente — la fiche n'exposait jusqu'ici que la tranche.")
        UUID projetId,

        @Schema(description = "Code du projet. 99 %.", example = "CITEB")
        String projetCode,

        @Schema(description = "Libellé du projet. 99 %.")
        String projetLibelle,

        String cin,

        UUID dossierId,

        @Schema(description = "Code du dossier issu de la prospection. `null` si non transformée.",
                example = "26/00314")
        String dossierCode,

        @Schema(description = "Identifiant du client, une fois la prospection transformée. `null` "
                + "sinon.")
        UUID clientId,

        @Schema(description = "Statut du dossier issu de la prospection. `null` si non "
                + "transformée.",
                example = "CONCRETISATION")
        String statutDossier
) {
}
