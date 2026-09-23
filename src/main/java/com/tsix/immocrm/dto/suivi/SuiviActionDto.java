package com.tsix.immocrm.dto.suivi;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneSuiviAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une ligne de {@code GET /api/suivi-actions} — une action commerciale ({@code ComActionMessage}),
 * avec les colonnes de l'écran OMRAN « Suivi des actions ».
 *
 * <p>Filtré par {@code ?colonnes=} : seuls les champs demandés sont sérialisés — voir
 * {@link ColonneSuiviAction}, dont les noms sont exactement ceux des composants ci-dessous.
 */
@JsonFilter(ColonneSuiviAction.FILTRE)
@Schema(description = "Une action commerciale (appel, visite, message, note…) telle que l'affiche "
        + "l'écran OMRAN « Suivi des actions ». Seules les colonnes demandées par `?colonnes=` sont "
        + "présentes — catalogue : `GET /api/referentiels/colonnes/suivi-actions`.")
public record SuiviActionDto(
        @Schema(description = "Identifiant de l'action (ComActionMessage.PKey). Toujours servi.")
        UUID id,
        @Schema(description = "Auteur de l'action tel qu'écrit sur la ligne (`UserName`) — la colonne "
                + "sur laquelle filtre l'écran OMRAN. « Système » ou vide pour une action générée.",
                example = "Khadija ELKORRI")
        String utilisateur,
        @Schema(description = "Type d'action (ComActionMessageType).", example = "Téléphone Sortant")
        String typeAction,
        @Schema(description = "Rang de l'action parmi celles du même type pour le même tiers, **1 = la "
                + "plus récente** (hors actions système et impayées) — la règle de la vue OMRAN. "
                + "Calculé pour la page servie : ni triable ni groupable.", example = "1")
        Integer rang,
        @Schema(description = "Date de l'action (`DocDate`).")
        LocalDateTime dateAction,
        @Schema(description = "Date de l'alerte / de la relance (`AlertValidityDate`). Null si aucune.")
        LocalDateTime dateAlerte,
        @Schema(description = "L'alerte de cette action est-elle encore active (`AdmAlertData.Active`) ? "
                + "`false` si aucune alerte. Calculé pour la page servie.")
        Boolean alerteActive,
        @Schema(description = "Catégorie (Prospection, Dossier de vente…).", example = "Prospection")
        String categorie,
        @Schema(description = "Résultat (sous-catégorie).", example = "Rendez vous pris")
        String resultat,
        @Schema(description = "Note saisie (`AlertMessage`).")
        String message,
        @Schema(description = "Action marquée impayé (`IsUnpaid`).")
        Boolean impaye,
        @Schema(description = "Action générée par le système (`IsSystem`) — relance, événement… "
                + "Listée comme dans OMRAN ; filtrable par `?systeme=false`.")
        Boolean systeme,
        @Schema(description = "Action entrante (`IsIn`). Rarement renseigné : le type porte le sens.")
        Boolean entrante,
        @Schema(description = "Commercial concerné (`CfgEmployeeId` → `CfgTier.Description`) — pas "
                + "l'auteur, qui est `utilisateur`. Vide sur 23 % des actions réelles.")
        String commercial,
        @Schema(description = "Personnel : employé du compte auteur, sinon le compte, sinon le "
                + "commercial — règle de la vue OMRAN. Calculé pour la page servie.")
        String personnel,
        @Schema(description = "Tiers (prospect/client) de l'action (`CfgTier.Description`).")
        String tiersNom,
        @Schema(description = "Code de la prospection rattachée.", example = "26/05947")
        String prospectionCode,
        @Schema(description = "Code du dossier de vente rattaché.")
        String dossierCode,
        @Schema(description = "État : statut du dossier de l'action, sinon du dossier né de sa "
                + "prospection, sinon sa catégorie — règle de la vue OMRAN. Calculé pour la page servie.")
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
