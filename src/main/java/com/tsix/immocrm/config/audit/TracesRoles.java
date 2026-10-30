package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.adm.AdmRole;

import java.util.List;

/** {@code AdmRight} n'en fait pas partie : un remplacement des droits s'écrit en une ligne de résumé. */
public class TracesRoles implements TracesAudit {

    private static final String TYPE = "Yes.Adm.AdmRole";

    @Override
    public List<Class<?>> classes() {
        return List.of(AdmRole.class);
    }

    @Override
    public List<String> types() {
        return List.of(TYPE);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof AdmRole r ? new Trace(TYPE, "Rôle", r.getCode(), null, null) : null;
    }
}
