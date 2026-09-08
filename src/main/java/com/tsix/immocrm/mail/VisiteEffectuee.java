package com.tsix.immocrm.mail;

import java.util.UUID;

/**
 * Événement applicatif : une prospection vient de passer à l'étape « 4 APRES VISITES ». Publié par
 * {@code ProspectionService#changerEtape}, consommé après commit par
 * {@link RemerciementVisiteListener}.
 *
 * <p><b>Pourquoi le passage d'étape et non un résultat d'interaction.</b> Aucun résultat ne
 * signifie « visite effectuée » : {@code Réalisée} et {@code Non réalisée} existent bien dans
 * {@code ComActionMessageSubCategory}, mais comptent <b>zéro ligne</b> sur les 44 152 interactions
 * réelles. Le passage d'étape est le seul signal disponible, et il n'a pas de concurrent.
 *
 * <p>Comme {@link VisitePlanifiee}, l'événement ne transporte que l'identifiant : tout le contenu
 * du remerciement est en base, et le relire dans l'écouteur donne la même résolution à l'envoi
 * automatique qu'à un éventuel renvoi manuel.
 *
 * @param prospectionId {@code ComProspection.PKey} de la prospection passée à l'étape
 */
public record VisiteEffectuee(UUID prospectionId) {
}
