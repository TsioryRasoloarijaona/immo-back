package com.tsix.immocrm.whatsapp;

import org.springframework.util.StringUtils;

/**
 * Masquage d'un numéro de téléphone pour les journaux : {@code +212661224857} devient
 * {@code +212******57}.
 *
 * <p>Pendant du {@code AdresseMasquee} du package mail, et pour la même raison : tracer qu'une
 * confirmation est partie est utile, consigner le numéro personnel de chaque prospect dans un
 * fichier de log archivé et recopié l'est beaucoup moins. Un numéro de téléphone est une donnée
 * personnelle directement identifiante — davantage qu'une adresse email professionnelle.
 *
 * <p>Ce qui est conservé l'est délibérément : <b>l'indicatif</b>, qui permet de repérer un
 * numéro étranger là où on attendait un marocain, et <b>les deux derniers chiffres</b>, qui
 * suffisent à rapprocher une trace d'une fiche quand on la cherche.
 */
public final class NumeroMasque {

    private NumeroMasque() {
    }

    public static String de(String numero) {
        if (!StringUtils.hasText(numero)) {
            return null;
        }
        String propre = numero.trim();
        // Trop court pour masquer quoi que ce soit d'utile : on ne laisse rien filtrer.
        if (propre.length() < 8) {
            return "***";
        }
        String debut = propre.startsWith("+") ? propre.substring(0, 4) : propre.substring(0, 3);
        String fin = propre.substring(propre.length() - 2);
        return debut + "*".repeat(propre.length() - debut.length() - 2) + fin;
    }
}
