package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Résultat d'une réinitialisation de mot de passe par un administrateur.")
public record ReinitialiserMotDePasseResponse(

        UUID admUserId,

        @Schema(description = "Nom d'utilisateur (AdmUser.Code) du compte réinitialisé.")
        String username,

        @Schema(description = "Mot de passe temporaire généré, envoyé par email à l'utilisateur "
                + "et renvoyé ici une seule fois (jamais une seconde, ni stocké en clair côté "
                + "serveur). Le compte devra le changer dès sa prochaine connexion "
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
                + "d'œil que le message part au bon endroit. Résolue depuis `AdmUser.Email`, à "
                + "défaut depuis l'email de la fiche employé (`CfgTier`).",
                example = "j***@exemple.ma")
        String destinataire
) {
}
