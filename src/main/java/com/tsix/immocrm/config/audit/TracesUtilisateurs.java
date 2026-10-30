package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.adm.AdmUser;

import java.util.List;

/** {@code AdmUserRole} et {@code StlDepositUser} n'en font pas partie : un remplacement s'écrit en une ligne de résumé. */
public class TracesUtilisateurs implements TracesAudit {

    private static final String TYPE = "Yes.Adm.AdmUser";

    @Override
    public List<Class<?>> classes() {
        return List.of(AdmUser.class);
    }

    @Override
    public List<String> types() {
        return List.of(TYPE);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof AdmUser u ? new Trace(TYPE, "Utilisateur", u.getCode(), null, null) : null;
    }
}
