package com.tsix.immocrm.domain;

/**
 * À qui s'adresse la liste de relances de {@code GET /api/dashboard/relances}.
 *
 * <p>Les deux valeurs se posent <b>par-dessus</b> le périmètre commercial, elles ne le
 * remplacent pas : {@link #EQUIPE} ne montre jamais plus que ce que le périmètre autorise déjà.
 *
 * <p><b>Dans les deux cas, les relances non affectées sont servies</b> — celles dont ni la
 * prospection ni l'interaction ne désigne un commercial (108 sur 3 765 en base réelle, dont les
 * 21 posées sur des pré-prospections, orphelines par construction). Les réserver au périmètre de
 * quelqu'un reviendrait à les enterrer, or c'est le canal des leads injectés automatiquement.
 * Voir {@code instructions/FEATURE-dashboard-relances.md} §4.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
 */
public enum PorteeRelance {

    /** Les relances dont le commercial résolu est l'appelant, plus les non affectées. */
    MOI,

    /** Tout le périmètre visible de l'appelant, sans distinction de titulaire. */
    EQUIPE
}
