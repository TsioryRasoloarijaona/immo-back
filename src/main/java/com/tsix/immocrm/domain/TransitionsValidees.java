package com.tsix.immocrm.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tsix.immocrm.domain.StatutDossier.*;

/**
 * Transitions de {@link StatutDossier} imposées par le service (409). Empiriques : paires observées
 * dans {@code StrFolderStatusHistory}, auto-transitions exclues, retours arrière gardés sur confirmation.
 * // TODO: LIVRAISON_PROVISOIRE transition non confirmée (jamais observée, placée après FORMALISE)
 */
public final class TransitionsValidees {

    private static final Map<StatutDossier, Set<StatutDossier>> TRANSITIONS = new EnumMap<>(StatutDossier.class);

    static {
        TRANSITIONS.put(RESERVATION, EnumSet.of(FORMALISE, CONCRETISATION, ANNULATION, DESISTEMENT));
        TRANSITIONS.put(CONCRETISATION, EnumSet.of(ENVOI_AU_NOTAIRE, RESERVATION, DESISTEMENT, MEILLEURTAUX));
        TRANSITIONS.put(MEILLEURTAUX, EnumSet.of(ENVOI_AU_NOTAIRE));
        TRANSITIONS.put(ENVOI_AU_NOTAIRE, EnumSet.of(MINUTE, COMPROMIS, DESISTEMENT, CREDIT_EN_COURS, CONCRETISATION, ANNULATION));
        TRANSITIONS.put(COMPROMIS, EnumSet.of(CREDIT_EN_COURS));
        TRANSITIONS.put(CREDIT_EN_COURS, EnumSet.of(CREDIT_DEBLOQUE, ENVOI_AU_NOTAIRE, MINUTE));
        TRANSITIONS.put(CREDIT_DEBLOQUE, EnumSet.of(MINUTE, ENVOI_AU_NOTAIRE));
        TRANSITIONS.put(MINUTE, EnumSet.of(REMISE_DES_CLEFS, ENVOI_AU_NOTAIRE));
        TRANSITIONS.put(REMISE_DES_CLEFS, EnumSet.of(FORMALISE));
        TRANSITIONS.put(FORMALISE, EnumSet.of(REMISE_DES_CLEFS, RESERVATION, MINUTE, LIVRAISON_PROVISOIRE));
        TRANSITIONS.put(ANNULATION, EnumSet.of(RESERVATION));
        TRANSITIONS.put(DESISTEMENT, EnumSet.noneOf(StatutDossier.class));
        TRANSITIONS.put(LIVRAISON_PROVISOIRE, EnumSet.noneOf(StatutDossier.class));
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
