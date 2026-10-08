package com.tsix.immocrm.whatsapp;

import org.springframework.util.StringUtils;

/**
 * Pour les journaux : {@code +212661224857} → {@code +212******57}. L'indicatif trahit un numéro étranger, les deux
 * derniers chiffres suffisent à retrouver la fiche.
 */
public final class NumeroMasque {

    private NumeroMasque() {
    }

    public static String de(String numero) {
        if (!StringUtils.hasText(numero)) {
            return null;
        }
        String propre = numero.trim();
        // Trop court pour masquer utilement : ne rien laisser filtrer.
        if (propre.length() < 8) {
            return "***";
        }
        String debut = propre.startsWith("+") ? propre.substring(0, 4) : propre.substring(0, 3);
        String fin = propre.substring(propre.length() - 2);
        return debut + "*".repeat(propre.length() - debut.length() - 2) + fin;
    }
}
