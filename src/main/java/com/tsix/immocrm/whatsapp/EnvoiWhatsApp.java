package com.tsix.immocrm.whatsapp;

/** Port d'envoi WhatsApp : la suite de tests ne doit jamais appeler UltraMsg ni écrire à un vrai numéro lu en base. */
public interface EnvoiWhatsApp {

    /**
     * Ne lève jamais. Booléen, contrairement à l'email : UltraMsg dit de façon synchrone s'il accepte le message.
     *
     * @return {@code true} si l'API a accepté le message
     */
    boolean envoyer(MessageWhatsApp message);

    /** Faux si {@code app.whatsapp.actif} est baissé ou si jeton ou instance manquent. */
    boolean estActif();
}
