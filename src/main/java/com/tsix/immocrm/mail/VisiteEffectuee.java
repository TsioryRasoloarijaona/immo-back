package com.tsix.immocrm.mail;

import java.util.UUID;

/**
 * Passage à l'étape « 4 APRES VISITES », seul signal de visite effectuée : les résultats {@code Réalisée} /
 * {@code Non réalisée} ne sont jamais saisis (FEATURE-offre-apres-visite.md §1). Identifiant seul : relu en base.
 */
public record VisiteEffectuee(UUID prospectionId) {
}
