package com.tsix.immocrm.mail;

import java.util.Map;

/**
 * Un message à envoyer, décrit par son template et ses variables — pas par son HTML.
 * Le rendu appartient à l'implémentation du port ({@link EnvoiEmailSmtp}), de sorte qu'un
 * double de test puisse vérifier les <i>variables</i> transmises plutôt que d'avoir à
 * inspecter du balisage.
 *
 * @param destinataire adresse du destinataire, déjà résolue et validée par l'appelant
 * @param sujet        objet du message, en français
 * @param template     chemin du template Thymeleaf sans extension, ex.
 *                     {@code mail/mot-de-passe-temporaire}
 * @param variables    variables du template
 * @param corpsTexte   version texte brut, envoyée en alternative multipart pour les clients
 *                     qui n'affichent pas le HTML. {@code null} = message HTML seul.
 */
public record MessageEmail(
        String destinataire,
        String sujet,
        String template,
        Map<String, Object> variables,
        String corpsTexte
) {
}
