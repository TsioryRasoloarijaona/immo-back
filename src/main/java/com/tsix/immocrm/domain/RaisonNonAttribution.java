package com.tsix.immocrm.domain;

/** Pourquoi une pré-prospection reste « à distribuer » après la répartition automatique. */
public enum RaisonNonAttribution {
    /** Sans tranche, ou tranche sans projet. */
    SANS_PROJET,
    /** Aucun commercial actif habilité explicitement sur le projet. */
    AUCUN_ELIGIBLE,
    /** Elle a déjà un commercial : la répartition ne réattribue jamais. */
    DEJA_ATTRIBUEE
}
