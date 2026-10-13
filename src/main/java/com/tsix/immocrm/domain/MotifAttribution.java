package com.tsix.immocrm.domain;

/** Pourquoi la répartition automatique a choisi ce commercial (FEATURE-repartition-notifications.md §3.1). */
public enum MotifAttribution {
    /** Même téléphone ou e-mail qu'une prospection ouverte ou un prospect déjà suivi par lui. */
    CONNU,
    /** Le moins de leads à trier parmi les éligibles du projet. */
    MOINS_CHARGE
}
