package com.tsix.immocrm.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tsix.immocrm.domain.StatutDossier.*;

/**
 * Transitions de {@link StatutDossier} imposées par le service (409) : copie des 34 lignes
 * {@code StrWorkFlow} de l'entité dossier de vente (FEATURE-vente-socle.md B2), auto-transitions exclues.
 */
public final class TransitionsValidees {

    private static final Map<StatutDossier, Set<StatutDossier>> TRANSITIONS = new EnumMap<>(StatutDossier.class);

    static {
        TRANSITIONS.put(RESERVATION, EnumSet.of(FORMALISE, CONCRETISATION, ANNULATION, DESISTEMENT));
        TRANSITIONS.put(CONCRETISATION, EnumSet.of(ENVOI_AU_NOTAIRE, RESERVATION, DESISTEMENT, MEILLEURTAUX));
        TRANSITIONS.put(MEILLEURTAUX, EnumSet.of(ENVOI_AU_NOTAIRE, DESISTEMENT));
        TRANSITIONS.put(ENVOI_AU_NOTAIRE, EnumSet.of(MINUTE, COMPROMIS, DESISTEMENT, CONCRETISATION, ANNULATION,
                LIVRAISON_PROVISOIRE));
        TRANSITIONS.put(COMPROMIS, EnumSet.of(CREDIT_EN_COURS, DESISTEMENT));
        TRANSITIONS.put(CREDIT_EN_COURS, EnumSet.of(CREDIT_DEBLOQUE, ENVOI_AU_NOTAIRE, MINUTE, CONCRETISATION,
                DESISTEMENT));
        TRANSITIONS.put(CREDIT_DEBLOQUE, EnumSet.of(MINUTE, ENVOI_AU_NOTAIRE, DESISTEMENT));
        TRANSITIONS.put(MINUTE, EnumSet.of(REMISE_DES_CLEFS, ENVOI_AU_NOTAIRE));
        TRANSITIONS.put(REMISE_DES_CLEFS, EnumSet.of(FORMALISE));
        TRANSITIONS.put(FORMALISE, EnumSet.of(REMISE_DES_CLEFS, RESERVATION, MINUTE));
        TRANSITIONS.put(ANNULATION, EnumSet.of(RESERVATION));
        TRANSITIONS.put(DESISTEMENT, EnumSet.noneOf(StatutDossier.class));
        TRANSITIONS.put(LIVRAISON_PROVISOIRE, EnumSet.of(MINUTE));
    }

    private TransitionsValidees() {
    }

    public static boolean estValide(StatutDossier depuis, StatutDossier vers) {
        return TRANSITIONS.getOrDefault(depuis, Set.of()).contains(vers);
    }

    public static Set<StatutDossier> transitionsAutorisees(StatutDossier depuis) {
        return Set.copyOf(TRANSITIONS.getOrDefault(depuis, Set.of()));
    }
}
