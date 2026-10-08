package com.tsix.immocrm.mail;

import java.util.UUID;

/**
 * Rendez-vous planifié par une interaction ; publié pour qu'aucun mail ne parte d'une transaction annulée. Identifiant
 * seul (contrairement au mot de passe, qui n'existe qu'en mémoire) : envoi et renvoi manuel relisent la même base.
 *
 * @param interactionId {@code ComActionMessage.PKey}
 */
public record VisitePlanifiee(UUID interactionId) {
}
