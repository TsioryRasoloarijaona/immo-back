package com.tsix.immocrm.mail;

/**
 * Port d'envoi d'email. Une seule implémentation en production ({@link EnvoiEmailSmtp}) ;
 * l'intérêt de l'interface est de garder les tests hors du réseau — la suite tape une vraie
 * base de données mais ne doit <b>jamais</b> taper le vrai SMTP Hostinger. C'est le même
 * dispositif que {@code StockageFichiers} face à MinIO.
 */
public interface EnvoiEmail {

    /**
     * Envoie le message. <b>Ne lève jamais</b> : un SMTP injoignable est journalisé, pas
     * propagé. L'appelant est un écouteur d'événement exécuté après commit, hors de la
     * transaction et hors du fil de la requête — une exception y serait perdue de toute
     * façon, et rien ne doit être annulé au motif qu'un mail n'est pas parti.
     */
    void envoyer(MessageEmail message);

    /**
     * Le service est-il en mesure d'envoyer ? {@code false} si l'interrupteur
     * {@code app.mail.actif} est baissé ou si la configuration SMTP est incomplète.
     *
     * <p>Lu par les services appelants pour renseigner honnêtement le champ
     * {@code emailEnvoye} de leur réponse, plutôt que d'affirmer un envoi qui n'aura pas lieu.
     */
    boolean estActif();
}
