package com.tsix.immocrm.mail;

import org.springframework.util.StringUtils;

/**
 * {@code jean.dupont@ayad.ma} → {@code j***@ayad.ma}, pour les journaux et la réponse HTTP. Le domaine reste entier :
 * c'est lui qui trahit une adresse personnelle là où on attendait la professionnelle.
 */
public final class AdresseMasquee {

    private AdresseMasquee() {
    }

    public static String de(String adresse) {
        if (!StringUtils.hasText(adresse)) {
            return null;
        }
        int arobase = adresse.indexOf('@');
        // Pas une adresse : ne rien laisser filtrer pour autant.
        if (arobase < 1) {
            return "***";
        }
        return adresse.charAt(0) + "***" + adresse.substring(arobase);
    }
}
