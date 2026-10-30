package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.adm.AdmTask;

import java.util.List;

/** Écrite dans {@code AdmWebHistorics} sans {@code types()} : aucun écran ne lit ce journal, {@code /api/audit} n'exige aucun rôle. */
public class TracesTaches implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(AdmTask.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof AdmTask t ? new Trace("Yes.Adm.AdmTask", "Tâche", t.getDescription(), null, null) : null;
    }
}
