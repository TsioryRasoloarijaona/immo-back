package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.stl.StlSettlementLine;

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

    /**
     * Seule règle « compte dans les totaux » (FEATURE-lot4 §2) : JPQL sur l'alias {@code st} du
     * règlement, SQL sur l'alias {@code s} de {@code StlSettlement}, Java par {@link #compteDansLesTotaux}.
     */
    public static final String JPQL_COMPTE_DANS_LES_TOTAUX = "(st.status IS NULL OR st.status <> 'CANCELED')";
    public static final String SQL_COMPTE_DANS_LES_TOTAUX = "(s.Status IS NULL OR s.Status <> 'CANCELED')";

    /** NULL, IN_WORK, POINTED ou valeur inconnue comptent ; seul CANCELED est exclu. */
    public static boolean compteDansLesTotaux(String status) {
        return lireCode(status).filter(statut -> statut == CANCELED).isEmpty();
    }

    /** La ligne compte si son règlement compte (ligne sans règlement : comptée). */
    public static boolean compteDansLesTotaux(StlSettlementLine ligne) {
        return ligne.getStlSettlement() == null || compteDansLesTotaux(ligne.getStlSettlement().getStatus());
    }

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
