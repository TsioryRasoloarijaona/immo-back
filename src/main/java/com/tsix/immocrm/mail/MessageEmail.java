package com.tsix.immocrm.mail;

import java.util.List;
import java.util.Map;

/**
 * Un message à envoyer, décrit par son template et ses variables — pas par son HTML.
 * Le rendu appartient à l'implémentation du port ({@link EnvoiEmailSmtp}), de sorte qu'un
 * double de test puisse vérifier les <i>variables</i> transmises plutôt que d'avoir à
 * inspecter du balisage.
 *
 * @param destinataire  adresse du destinataire, déjà résolue et validée par l'appelant
 * @param sujet         objet du message, en français
 * @param template      chemin du template Thymeleaf sans extension, ex.
 *                      {@code mail/mot-de-passe-temporaire}
 * @param variables     variables du template
 * @param corpsTexte    version texte brut, envoyée en alternative multipart pour les clients
 *                      qui n'affichent pas le HTML. {@code null} = message HTML seul.
 * @param piecesJointes fichiers joints, déjà résolus en octets et déjà plafonnés par l'appelant
 *                      (voir {@link PieceJointeEmail}). Jamais {@code null} : la liste vide est
 *                      la valeur normale, et c'est le cas de loin le plus fréquent.
 */
public record MessageEmail(
        String destinataire,
        String sujet,
        String template,
        Map<String, Object> variables,
        String corpsTexte,
        List<PieceJointeEmail> piecesJointes
) {

    public MessageEmail {
        piecesJointes = piecesJointes == null ? List.of() : List.copyOf(piecesJointes);
    }

    /** Message sans pièce jointe — la forme courante. */
    public MessageEmail(String destinataire, String sujet, String template,
                        Map<String, Object> variables, String corpsTexte) {
        this(destinataire, sujet, template, variables, corpsTexte, List.of());
    }
}
