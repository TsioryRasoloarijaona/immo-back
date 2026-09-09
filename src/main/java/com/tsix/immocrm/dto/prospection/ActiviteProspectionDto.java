package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bloc « activité » de la fiche prospection, tel que le calcule la vue légataire
 * {@code ComSaleProspectionView} et que l'affiche le logiciel de bureau.
 */
@Schema(description = """
        Indicateurs d'activité d'une prospection, calculés par la base légataire \
        (`ComSaleProspectionView`) et non stockés.

        **Le bloc entier vaut `null` sur une pré-prospection** : la vue qui le produit exclut par \
        construction les 3 817 pré-prospections, qui ont la leur. Afficher « 0 interaction » dans \
        ce cas serait faux — il faut afficher « non disponible ».""")
public record ActiviteProspectionDto(

        @Schema(description = "Nombre de jours écoulés depuis la dernière action. C'est le "
                + "critère de tri des relances du logiciel de bureau. Renseigné sur 100 % des "
                + "prospections de vente.",
                example = "1688")
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

        @Schema(description = "CIN du prospect, telle que la porte la vue. 99 %.")
        String cin,

        @Schema(description = "Identifiant du dossier de vente issu de cette prospection — **le "
                + "lien de conversion**, jusqu'ici absent de l'API. `null` tant que la "
                + "prospection n'a pas été transformée, ce qui est le cas de 98 % d'entre elles : "
                + "c'est le taux de conversion réel (358 dossiers pour 14 118 prospections), pas "
                + "une donnée manquante.")
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
