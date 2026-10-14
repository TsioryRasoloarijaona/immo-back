package com.tsix.immocrm.services.dossier.workflow;

import java.util.UUID;

/**
 * Une transition sortante du statut courant. {@code workflowId} nul = transition de repli
 * ({@link com.tsix.immocrm.domain.TransitionsValidees}, base sans paramétrage {@code StrWorkFlow}).
 */
public record TransitionDossier(UUID workflowId, String description, String codeCible, boolean automatique) {

    public boolean parametree() {
        return workflowId != null;
    }
}
