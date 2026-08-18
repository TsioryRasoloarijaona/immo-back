package com.tsix.immocrm.domain;

import java.util.UUID;

/**
 * Dossier (ComFolder) lifecycle status — 13 values + 2 exits.
 *
 * Confirmed via direct query against the real database: {@code StrStatus} rows where
 * {@code StrConfigEntityId = 'E7C73F70-7507-40D9-AA78-A2825C42B600'} (the Dossier config
 * entity), matching {@code ComFolder.StrStatusId} in practice. {@link #code} holds the exact
 * {@code StrStatus.Code} value for lookup/persistence.
 *
 * ANNULATION and DESISTEMENT are the "2 sorties" (exit states) — they sit outside the normal
 * sort sequence (30/31 vs. 1-9/71) and were confirmed as terminal by the transition history
 * (see {@link TransitionsValidees}).
 */
public enum StatutDossier {
    RESERVATION("RESERVATION"),
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

    /** {@code StrStatus.StrConfigEntityId} identifying the Dossier config entity (confirmed via DB). */
    public static final UUID CONFIG_ENTITY_ID = UUID.fromString("E7C73F70-7507-40D9-AA78-A2825C42B600");

    private final String code;

    StatutDossier(String code) {
        this.code = code;
    }

    /** The exact {@code StrStatus.Code} value in the database. */
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
