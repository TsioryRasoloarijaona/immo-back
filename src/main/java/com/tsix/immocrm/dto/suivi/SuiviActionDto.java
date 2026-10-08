package com.tsix.immocrm.dto.suivi;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneSuiviAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** Filtré par {@link ColonneSuiviAction#FILTRE} : les noms de colonnes y sont exactement ceux des composants. */
@JsonFilter(ColonneSuiviAction.FILTRE)
public record SuiviActionDto(
        @Schema(description = "Identifiant de l'action (ComActionMessage.PKey). Toujours servi.")
        UUID id,
        @Schema(example = "Khadija ELKORRI")
        String utilisateur,
        @Schema(description = "Type d'action (ComActionMessageType).", example = "Téléphone Sortant")
        String typeAction,
        @Schema(example = "1")
        Integer rang,
        @Schema(description = "Date de l'action (`DocDate`).")
        LocalDateTime dateAction,
        @Schema(description = "Date de l'alerte / de la relance (`AlertValidityDate`). Null si aucune.")
        LocalDateTime dateAlerte,
        Boolean alerteActive,
        @Schema(description = "Catégorie (Prospection, Dossier de vente…).", example = "Prospection")
        String categorie,
        @Schema(description = "Résultat (sous-catégorie).", example = "Rendez vous pris")
        String resultat,
        @Schema(description = "Note saisie (`AlertMessage`).")
        String message,
        @Schema(description = "Action marquée impayé (`IsUnpaid`).")
        Boolean impaye,
        Boolean systeme,
        @Schema(description = "Action entrante (`IsIn`). Rarement renseigné : le type porte le sens.")
        Boolean entrante,
        String commercial,
        String personnel,
        String tiersNom,
        @Schema(description = "Code de la prospection rattachée.", example = "26/05947")
        String prospectionCode,
        @Schema(description = "Code du dossier de vente rattaché.")
        String dossierCode,
        String etat,
        @Schema(description = "Code de l'article rattaché.")
        String articleCode,
        @Schema(description = "Libellé de l'article rattaché.")
        String articleLibelle,
        @Schema(description = "Tranche **résolue comme OMRAN** : catégorie Prospection → tranche de la "
                + "prospection, sinon tranche de l'article.", example = "APF")
        String trancheCode,
        @Schema(description = "Libellé de la tranche résolue.")
        String trancheLibelle,
        @Schema(description = "Programme (projet) de la tranche résolue.")
        String programme,
        @Schema(description = "Entité juridique du programme.")
        String entite,
        @Schema(description = "Id du type d'action.")
        UUID typeId,
        @Schema(description = "Id de la catégorie.")
        UUID categorieId,
        @Schema(description = "Id du tiers — ouvrir la fiche prospect/client.")
        UUID tiersId,
        @Schema(description = "Id de la prospection — `GET /api/prospections/{id}`.")
        UUID prospectionId,
        @Schema(description = "Id du dossier — `GET /api/dossiers/{id}`.")
        UUID dossierId,
        @Schema(description = "Id de la tranche résolue.")
        UUID trancheId,
        @Schema(description = "Créé par (`UserCreate`).")
        String creePar,
        @Schema(description = "Créé le (`UserCreateDateTime`).")
        LocalDateTime creeLe,
        @Schema(description = "Modifié par (`UserUpdate`).")
        String modifiePar,
        @Schema(description = "Modifié le (`UserUpdateDateTime`).")
        LocalDateTime modifieLe
) {
}
