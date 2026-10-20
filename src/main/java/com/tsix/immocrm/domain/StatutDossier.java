package com.tsix.immocrm.domain;

import java.util.UUID;

/**
 * Statut du dossier : {@code code} = {@code StrStatus.Code} exact, lignes de l'entité de config
 * {@link #CONFIG_ENTITY_ID}. ANNULATION et DESISTEMENT sont les deux sorties, hors de l'ordre de tri.
 */
public enum StatutDossier {
    RESERVATION(StatutDossier.CODE_RESERVATION),
    CONCRETISATION("CONCRETISATION"),
    MEILLEURTAUX("MEILLEURTAUX"),
    COMPROMIS("COMPROMIS"),
    ENVOI_AU_NOTAIRE("ENVOI AU NOTAIRE"),
    CREDIT_EN_COURS("CREDIT EN COURS"),
    CREDIT_DEBLOQUE("CREDIT DEBLOQUE"),
    MINUTE("MINUTE"),
    REMISE_DES_CLEFS("REMISE DES CLEFS"),
    FORMALISE("FORMALISE"),
    LIVRAISON_PROVISOIRE("LIVRAISON PROVISOIRE"),
    ANNULATION("ANNULATION"),
    DESISTEMENT("DESISTEMENT");

    /** Constante de compilation : lue par le {@code @Subselect} de {@code StkItemEtatVentes}. */
    public static final String CODE_RESERVATION = "RESERVATION";

    public static final UUID CONFIG_ENTITY_ID = UUID.fromString("E7C73F70-7507-40D9-AA78-A2825C42B600");

    private final String code;

    StatutDossier(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isSortie() {
        return this == ANNULATION || this == DESISTEMENT;
    }

    public static StatutDossier fromCode(String code) {
        for (StatutDossier statut : values()) {
            if (statut.code.equalsIgnoreCase(code)) {
                return statut;
            }
        }
        throw new IllegalArgumentException("Code StatutDossier inconnu: " + code);
    }
}
