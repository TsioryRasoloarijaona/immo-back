package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;

import java.util.List;

/**
 * Entités qu'un domaine trace dans {@code AdmWebHistorics}. Un lot qui écrit une nouvelle table ajoute sa classe
 * publique dans {@code config.audit} ; {@link RegistreAuditOmran} la trouve sans liste centrale.
 */
public interface TracesAudit {

    /** Classes exactes tracées (une sous-classe ne l'est pas d'office). */
    List<Class<?>> classes();

    /** Types que le journal rend ; une trace absente d'ici s'écrit mais ne sort pas de {@code AdmWebHistorics}. */
    default List<String> types() {
        return List.of();
    }

    /** {@code null} si l'entité n'est pas de ce domaine. */
    Trace tracer(Object entite);
}
