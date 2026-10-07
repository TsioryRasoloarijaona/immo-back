package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/**
 * {@code articleId} doit appartenir à {@code trancheId} (400 sinon) ; absent, l'article courant
 * qui n'appartient pas à la nouvelle tranche est désassigné.
 */
public record ModifierTranchePrincipaleRequest(
        UUID trancheId,
        UUID articleId
) {
}
