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

        @Schema(description = "Id de l'employé lié (`CfgEmployeeId`). Renseigné sur 77 % des "
                + "interactions réelles seulement — `null` ne veut pas dire auteur inconnu, "
                + "voir `employeNom`.")
        UUID employeId,

        @Schema(description = "**Nom d'affichage de l'auteur — toujours renseigné quand la "
                + "base le connaît (99,996 % des lignes réelles).** L'employé lié quand il "
                + "existe, sinon le compte qui a saisi l'interaction (`UserName`, puis "
                + "`UserCreate`) — c'est ce que le logiciel de bureau affiche. Ne PAS afficher "
                + "de repli type « Système » côté client sur la seule absence d'`employeId`.",
                example = "Khadija ELKORRI")
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
        LocalDateTime dateRdv,

        @Schema(description = "Id de la **catégorie** de l'interaction — à ne pas confondre avec "
                + "`resultatId`, qui est la sous-catégorie. Renseignée sur 100 % des 48 160 "
                + "interactions réelles.")
        UUID categorieId,

        @Schema(description = "Libellé de la catégorie. Sert de discriminant : `Prospection` "
                + "(46 666 lignes), `Dossier de vente (Réservation)` (1 325), "
                + "`Dossier de vente (Livraison)` (167), `Réclamation` (2).",
                example = "Prospection")
        String categorie,

        @Schema(description = "Id de la tranche de rattachement. Déduite comme le fait le "
                + "logiciel de bureau : la tranche de la **prospection** quand `categorie` vaut "
                + "\"Prospection\", celle du **lot** sinon. Renseignée sur 96 % des interactions ; "
                + "`null` quand ni l'une ni l'autre n'en porte.")
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
