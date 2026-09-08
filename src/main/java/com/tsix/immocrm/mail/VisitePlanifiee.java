package com.tsix.immocrm.mail;

import java.util.UUID;

/**
 * Événement applicatif : une interaction vient de planifier un rendez-vous, et un
 * {@code AdmEvent} a été créé pour lui. Publié par {@code ProspectionService#addInteraction},
 * consommé après commit par {@link EmailVisiteListener}.
 *
 * <p>Comme {@link MotDePasseTemporaireEmis}, passer par un événement sert une propriété précise :
 * <b>aucun mail ne part pour une transaction annulée</b>. Le commercial dont l'enregistrement
 * échoue ne doit pas avoir convoqué son prospect.
 *
 * <p><b>L'événement ne transporte que l'identifiant</b>, contrairement à
 * {@link MotDePasseTemporaireEmis} qui porte tout son contenu. La raison est asymétrique : un mot
 * de passe temporaire n'existe qu'en mémoire et ne peut donc pas être relu, alors que tout le
 * contenu d'une confirmation de visite est en base. Le relire dans l'écouteur donne la même
 * résolution — même code, mêmes règles — à l'envoi automatique et au renvoi manuel, qui n'a lui
 * aucun événement à sa disposition.
 *
 * @param interactionId {@code ComActionMessage.PKey} de l'interaction qui porte le rendez-vous
 */
public record VisitePlanifiee(UUID interactionId) {
}
