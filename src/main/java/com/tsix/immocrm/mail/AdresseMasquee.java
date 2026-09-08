package com.tsix.immocrm.mail;

import org.springframework.util.StringUtils;

/**
 * Masquage d'une adresse email : {@code jean.dupont@ayad.ma} devient {@code j***@ayad.ma}.
 *
 * <p>Deux usages, une seule règle pour les deux :
 * <ul>
 *   <li><b>Les journaux</b> — tracer qu'un mot de passe temporaire est parti est utile ;
 *       consigner l'adresse complète de chaque employé dans un fichier de log qui sera
 *       archivé et recopié l'est beaucoup moins.</li>
 *   <li><b>La réponse HTTP</b> — l'administrateur doit pouvoir vérifier d'un coup d'œil que
 *       le message part au bon endroit, ce qu'un domaine et une initiale suffisent à établir.</li>
 * </ul>
 *
 * <p>Le domaine est conservé en entier : c'est lui qui permet de repérer l'erreur qu'on
 * cherche à voir (une adresse personnelle là où on attendait l'adresse professionnelle).
 */
public final class AdresseMasquee {

    private AdresseMasquee() {
    }

    public static String de(String adresse) {
        if (!StringUtils.hasText(adresse)) {
            return null;
        }
        int arobase = adresse.indexOf('@');
        // Pas d'arobase : ce n'est pas une adresse, on ne laisse rien filtrer pour autant.
        if (arobase < 1) {
            return "***";
        }
        return adresse.charAt(0) + "***" + adresse.substring(arobase);
    }
}
