package com.tsix.immocrm.domain;

/**
 * Issue d'une tentative d'envoi de la confirmation de visite, <b>par canal</b>.
 *
 * <p>Rendu tel quel par l'endpoint de renvoi. Le commercial doit pouvoir distinguer ce qui se
 * corrige dans la fiche du prospect de ce qui relève de l'exploitation : les deux se soldent par
 * un message non parti, mais l'un se règle en saisissant une adresse ou un numéro, l'autre en
 * rallumant un service.
 */
public enum MotifEnvoiVisite {

    /** Le message a été remis au canal. */
    ENVOYE,

    /**
     * Aucune adresse email exploitable — ni la fiche ni {@code ComProspection.TempEMail}. C'est
     * le cas majoritaire des non-envois par mail : 43 % des prospections ayant un rendez-vous
     * n'ont aucune adresse, et 33 lignes réelles en portent une qui n'en est pas une.
     */
    DESTINATAIRE_ABSENT,

    /**
     * Un numéro existe sur la fiche mais ne se laisse pas mettre en forme internationale :
     * longueur anormale, ou numéro fixe. 43 cas réels sur 5 070, contre un seul prospect sans
     * aucun numéro. Distinct de {@link #DESTINATAIRE_ABSENT} parce que l'action corrective n'est
     * pas la même — ici il y a une saisie à réparer, pas une information à obtenir.
     */
    NUMERO_INEXPLOITABLE,

    /**
     * Le canal est globalement inactif — interrupteur baissé ou configuration incomplète
     * (SMTP sans identifiants, WhatsApp sans {@code WP_TOKEN}). Rien n'est reproché à la
     * prospection.
     */
    SERVICE_INACTIF,

    /**
     * Le canal était actif, le destinataire valide, et l'envoi a malgré tout échoué — API
     * injoignable, quota dépassé, instance WhatsApp déconnectée du téléphone. Le détail est
     * dans le journal du serveur ; le renvoi est possible une fois la cause levée.
     */
    ECHEC_ENVOI
}
