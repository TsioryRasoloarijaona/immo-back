package com.tsix.immocrm.domain;

/**
 * Issue d'un envoi de confirmation de visite, par canal : distingue ce qui se corrige dans la fiche
 * du prospect de ce qui relève de l'exploitation.
 */
public enum MotifEnvoiVisite {

    ENVOYE,

    /** Aucune adresse email exploitable — ni la fiche ni {@code ComProspection.TempEMail}. */
    DESTINATAIRE_ABSENT,

    /**
     * Un numéro existe mais ne se met pas en forme internationale (longueur anormale, fixe) :
     * une saisie à réparer, pas une information à obtenir.
     */
    NUMERO_INEXPLOITABLE,

    /** Canal inactif ou mal configuré (SMTP sans identifiants, WhatsApp sans {@code WP_TOKEN}). */
    SERVICE_INACTIF,

    /** Canal actif, destinataire valide, envoi échoué quand même : détail dans le journal du serveur. */
    ECHEC_ENVOI
}
