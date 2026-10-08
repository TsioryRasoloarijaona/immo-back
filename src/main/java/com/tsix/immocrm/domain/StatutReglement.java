package com.tsix.immocrm.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Statut d'un règlement ({@code StlSettlement.Status}, texte libre) et ses transitions, déduites du
 * cycle métier : {@code StlSettlementHistory} ne trace pas le statut. Valeurs et invariant
 * {@code POINTED} ⟺ {@code SettlementDate} : FEATURE-reglement-statut-preuve.md §2–§3.
 */
public enum StatutReglement {

    IN_WORK,
    POINTED,
    CANCELED;

    private static final Map<StatutReglement, Set<StatutReglement>> TRANSITIONS =
            new EnumMap<>(StatutReglement.class);

    static {
        TRANSITIONS.put(IN_WORK, EnumSet.of(POINTED, CANCELED));
        TRANSITIONS.put(POINTED, EnumSet.noneOf(StatutReglement.class));
        TRANSITIONS.put(CANCELED, EnumSet.noneOf(StatutReglement.class));
    }

    /** Statut d'un règlement qui vient d'être créé, comme les lignes legacy. */
    public static final StatutReglement INITIAL = IN_WORK;

    public static boolean estValide(StatutReglement depuis, StatutReglement vers) {
        return TRANSITIONS.getOrDefault(depuis, Set.of()).contains(vers);
    }

    public static Set<StatutReglement> transitionsAutorisees(StatutReglement depuis) {
        return Set.copyOf(TRANSITIONS.getOrDefault(depuis, Set.of()));
    }

    /**
     * Valeur absente ou inconnue → {@link Optional#empty()}, jamais d'exception : l'application
     * desktop écrit encore ce texte libre, et {@code GET /api/encaissements} ne doit pas échouer.
     */
    public static Optional<StatutReglement> lireCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        for (StatutReglement statut : values()) {
            if (statut.name().equalsIgnoreCase(code.trim())) {
                return Optional.of(statut);
            }
        }
        return Optional.empty();
    }

    /** Stricte, pour une valeur reçue d'un client : inconnue = erreur de requête. */
    public static StatutReglement depuisCode(String code) {
        return lireCode(code).orElseThrow(() ->
                new IllegalArgumentException("Statut de règlement inconnu: " + code));
    }
}
