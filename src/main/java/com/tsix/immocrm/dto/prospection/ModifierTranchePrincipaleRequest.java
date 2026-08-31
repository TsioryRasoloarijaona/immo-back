package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/**
 * {@code PATCH /api/prospections/{id}/tranche-principale} body — remplace la tranche/l'article
 * principaux d'une prospection en une seule opération atomique et cohérente. Contrairement au
 * {@code PATCH /api/prospections/{id}} générique (voir {@link ModifierProspectionRequest}), qui
 * ne validait pas la cohérence tranche/article, cet endpoint garantit que l'article appartient
 * bien à la tranche indiquée — voir {@code ProspectionService#modifierTranchePrincipale}.
 *
 * @param trancheId obligatoire — la nouvelle tranche principale.
 * @param articleId optionnel — le nouvel article principal, qui DOIT appartenir à {@code trancheId}
 *                  (400 sinon). Si absent et que l'article actuellement assigné n'appartient plus
 *                  à {@code trancheId}, il est automatiquement désassigné plutôt que laissé dans
 *                  un état incohérent.
 */
public record ModifierTranchePrincipaleRequest(
        UUID trancheId,
        UUID articleId
) {
}
