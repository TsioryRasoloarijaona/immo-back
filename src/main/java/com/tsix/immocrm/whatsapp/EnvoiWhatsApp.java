package com.tsix.immocrm.whatsapp;

/**
 * Port d'envoi WhatsApp. Une seule implémentation en production
 * ({@link EnvoiWhatsAppUltraMsg}) ; l'intérêt de l'interface est de garder les tests hors du
 * réseau — la suite tape une vraie base de données mais ne doit <b>jamais</b> appeler la vraie
 * API UltraMsg, ni expédier de message à un vrai numéro lu en base. Même dispositif que
 * {@code EnvoiEmail} face au SMTP et {@code StockageFichiers} face à MinIO.
 */
public interface EnvoiWhatsApp {

    /**
     * Envoie le message. <b>Ne lève jamais</b> : une API injoignable est journalisée, pas
     * propagée.
     *
     * <p><b>Renvoie un booléen, là où {@code EnvoiEmail#envoyer} ne renvoie rien</b> — et
     * l'asymétrie est délibérée. Remettre un message à un serveur SMTP ne dit rien de sa
     * livraison, donc un booléen y serait une fausse promesse. UltraMsg, lui, répond
     * synchroniquement en indiquant s'il a accepté le message : cette information existe, il
     * serait dommage de la jeter pour faire joli avec l'autre port.
     *
     * @return {@code true} si l'API a accepté le message
     */
    boolean envoyer(MessageWhatsApp message);

    /**
     * Le service est-il en mesure d'envoyer ? {@code false} si l'interrupteur
     * {@code app.whatsapp.actif} est baissé ou si le jeton / l'instance manquent.
     */
    boolean estActif();
}
