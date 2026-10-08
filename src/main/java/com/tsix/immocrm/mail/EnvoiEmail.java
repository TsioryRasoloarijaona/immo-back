package com.tsix.immocrm.mail;

/** Port d'envoi d'email : la suite de tests tape une vraie base, mais jamais le vrai SMTP. */
public interface EnvoiEmail {

    /** Ne lève jamais : appelé après commit hors du fil de la requête, rien ne doit être annulé pour un mail. */
    void envoyer(MessageEmail message);

    /** Lu par les appelants pour renseigner honnêtement {@code emailEnvoye} dans leur réponse. */
    boolean estActif();
}
