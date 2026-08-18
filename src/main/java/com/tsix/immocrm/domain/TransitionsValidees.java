package com.tsix.immocrm.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tsix.immocrm.domain.StatutDossier.*;

/**
 * Allowed {@link StatutDossier} transitions, enforced in the service layer on
 * {@code PATCH /api/dossiers/{id}/statut} (409 Conflict on violation).
 *
 * Derived by mining {@code StrFolderStatusHistory} on the real database — for each
 * {@code ComFolder}, ordering its status history by date and counting observed
 * (previous status → new status) pairs. Self-transitions (save without an actual status
 * change) were excluded as audit-log noise. This is empirical, not a documented rule: some
 * back-edges (e.g. {@code FORMALISE → RESERVATION}) look like manual corrections rather than
 * intended workflow, but were kept as-is per explicit confirmation.
 *
 * {@link StatutDossier#LIVRAISON_PROVISOIRE} has zero occurrences in the history — its only
 * transition here ({@code FORMALISE → LIVRAISON_PROVISOIRE}) is unconfirmed, placed on the
 * assumption it follows FORMALISE (adjacent sort order: 9 → 71).
 * // TODO: LIVRAISON_PROVISOIRE transition non confirmée
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
