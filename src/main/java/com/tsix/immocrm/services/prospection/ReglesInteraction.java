package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Champs obligatoires d'une interaction, règle OMRAN GEN0005 : note ⇒ message, autre action ⇒
 * résultat, résultat « avec alerte » ⇒ date d'alerte, résultat RDV ⇒ date de RDV (FEATURE-lot4 §4).
 */
public final class ReglesInteraction {

    static final String CODE_NOTE = "Note";

    /** Codes exacts des résultats qui exigent {@code dateRdv} et créent un {@code AdmEvent}. */
    static final Set<String> RESULTATS_AVEC_DATE_PLANIFIEE = Set.of(
            "Rendez vous pris",
            "RDV reporté",
            "Le prospect veut faire une visite du projet");

    private ReglesInteraction() {
    }

    static boolean estUneNote(ComActionMessageType actionType) {
        return actionType != null && actionType.getCode() != null
                && CODE_NOTE.equalsIgnoreCase(actionType.getCode().trim());
    }

    public static boolean exigeUneDateDeRdv(ComActionMessageSubCategory resultat) {
        return resultat != null && RESULTATS_AVEC_DATE_PLANIFIEE.contains(resultat.getCode());
    }

    /** 400 au premier champ manquant ; appelé avant toute écriture. */
    public static void verifier(ComActionMessageType actionType, ComActionMessageSubCategory resultat,
                         CreerInteractionRequest request) {
        if (actionType == null) {
            throw new InvalidRequestException("L'action (actionTypeId) est obligatoire.");
        }
        if (estUneNote(actionType)) {
            if (!StringUtils.hasText(request.description())) {
                throw new InvalidRequestException("Le message (description) est obligatoire pour une note.");
            }
        } else if (resultat == null) {
            throw new InvalidRequestException(
                    "Le résultat (resultatId) est obligatoire pour une action autre qu'une note.");
        }
        if (resultat != null && Boolean.TRUE.equals(resultat.getIsRevivalMandatory()) && request.dateAlerte() == null) {
            throw new InvalidRequestException("La date d'alerte (dateAlerte) est obligatoire pour le résultat \""
                    + resultat.getDescription() + "\".");
        }
        verifierDateRdv(resultat, request);
    }

    private static void verifierDateRdv(ComActionMessageSubCategory resultat, CreerInteractionRequest request) {
        if (exigeUneDateDeRdv(resultat) && request.dateRdv() == null) {
            throw new InvalidRequestException(
                    "dateRdv est requise lorsque le résultat est \"" + resultat.getCode() + "\"");
        }
        if (resultat == null && request.dateRdv() != null) {
            throw new InvalidRequestException("dateRdv nécessite un resultatId (\"Rendez vous pris\", "
                    + "\"RDV reporté\" ou \"Le prospect veut faire une visite du projet\")");
        }
    }
}
