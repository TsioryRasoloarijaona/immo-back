package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Résultat de la création d'un employé par un administrateur.")
public record CreerEmployeResponse(

        @Schema(description = "Id de la fiche personne créée (CfgTier).")
        UUID cfgTierId,

        @Schema(description = "Id du compte de connexion créé (AdmUser).")
        UUID admUserId,

        @Schema(description = "Nom d'utilisateur (AdmUser.Code) du compte créé.")
        String identifiant,

        @Schema(description = "Codes AdmRole assignés au compte.")
        List<String> roles,

        @Schema(description = "Mot de passe temporaire généré, envoyé par email à l'employé et "
                + "renvoyé ici une seule fois (jamais une seconde, ni stocké en clair côté "
                + "serveur). Le compte devra le changer dès sa première connexion "
                + "(mustChangePassword=true). **À continuer d'afficher à l'administrateur** : "
                + "l'envoi du mail est asynchrone et peut échouer, ce champ est le filet.")
        String motDePasseTemporaire,

        @Schema(description = "L'envoi du mail a été **déclenché**. L'expédition étant "
                + "asynchrone, `true` ne prouve pas la remise — seulement qu'aucun obstacle "
                + "n'a été rencontré côté serveur. `false` signifie que le service d'envoi est "
                + "inactif (configuration SMTP absente ou incomplète) : le mot de passe doit "
                + "alors être communiqué à la main.",
                example = "true")
        boolean emailEnvoye,

        @Schema(description = "Adresse de destination, masquée — permet de vérifier d'un coup "
                + "d'œil que le message part au bon endroit.", example = "j***@exemple.ma")
        String destinataire
) {
}
