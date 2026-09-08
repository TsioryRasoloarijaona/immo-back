package com.tsix.immocrm.domain;

/**
 * Issue d'une tentative d'envoi de la confirmation de visite.
 *
 * <p>Rendu tel quel par l'endpoint de renvoi. Le commercial doit pouvoir distinguer « le client
 * n'a pas d'adresse » de « le service mail est coupé » : les deux se soldent par un message non
 * parti, mais l'un se corrige en saisissant un email et l'autre relève de l'exploitation.
 */
public enum MotifEnvoiVisite {

    /** Le message a été remis au port d'envoi. */
    ENVOYE,

    /**
     * Ni la fiche du prospect ni {@code ComProspection.TempEMail} ne portent d'adresse
     * exploitable. C'est le cas majoritaire des non-envois : 43 % des prospections ayant un
     * rendez-vous n'ont aucune adresse, et 33 lignes réelles en portent une qui n'en est pas une.
     */
    DESTINATAIRE_ABSENT,

    /**
     * L'envoi d'email est globalement inactif — interrupteur {@code app.mail.actif} baissé ou
     * configuration SMTP incomplète. Rien n'est reproché à la prospection.
     */
    SERVICE_INACTIF
}
