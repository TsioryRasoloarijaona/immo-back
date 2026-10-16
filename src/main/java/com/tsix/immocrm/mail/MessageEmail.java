package com.tsix.immocrm.mail;

import java.util.List;
import java.util.Map;

/**
 * Template et variables, pas HTML : un double de test vérifie les variables sans lire de balisage.
 *
 * @param template      chemin Thymeleaf sans extension, ex. {@code mail/mot-de-passe-temporaire}
 * @param corpsTexte    alternative texte brut ; {@code null} = HTML seul
 * @param piecesJointes déjà résolues et plafonnées par l'appelant ; jamais {@code null}
 * @param copies        adresses en copie (Cc) ; jamais {@code null}
 */
public record MessageEmail(
        String destinataire,
        String sujet,
        String template,
        Map<String, Object> variables,
        String corpsTexte,
        List<PieceJointeEmail> piecesJointes,
        List<String> copies
) {

    public MessageEmail {
        piecesJointes = piecesJointes == null ? List.of() : List.copyOf(piecesJointes);
        copies = copies == null ? List.of() : List.copyOf(copies);
    }

    public MessageEmail(String destinataire, String sujet, String template,
                        Map<String, Object> variables, String corpsTexte, List<PieceJointeEmail> piecesJointes) {
        this(destinataire, sujet, template, variables, corpsTexte, piecesJointes, List.of());
    }

    public MessageEmail(String destinataire, String sujet, String template,
                        Map<String, Object> variables, String corpsTexte) {
        this(destinataire, sujet, template, variables, corpsTexte, List.of(), List.of());
    }
}
