package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.adm.AdmTask;

import java.util.List;

/** Le type se choisit par ligne : la table porte tâches et rendez-vous. Seuls les rendez-vous sortent par {@code /api/audit}. */
public class TracesTaches implements TracesAudit {

    private static final String TYPE_RENDEZ_VOUS = "Yes.Adm.AdmAppointment";

    @Override
    public List<Class<?>> classes() {
        return List.of(AdmTask.class);
    }

    @Override
    public List<String> types() {
        return List.of(TYPE_RENDEZ_VOUS);
    }

    @Override
    public Trace tracer(Object entite) {
        if (!(entite instanceof AdmTask t)) {
            return null;
        }
        return AdmTask.ENTITE_RENDEZ_VOUS.equals(t.getStrEntityId())
                ? new Trace(TYPE_RENDEZ_VOUS, "Rendez-vous", t.getDescription(), null, null)
                : new Trace("Yes.Adm.AdmTask", "Tâche", t.getDescription(), null, null);
    }
}
