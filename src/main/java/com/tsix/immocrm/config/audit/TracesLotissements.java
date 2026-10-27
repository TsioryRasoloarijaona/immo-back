package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;

import java.util.List;

/** Lot de terrain (FEATURE-patrimoine-lotissements.md §1.9) : StkItem déjà tracé, typé par {@link RegistreAuditOmran#article}. */
public class TracesLotissements implements TracesAudit {

    static final String LOT = "Yes.Stk.Land.Land";

    @Override
    public List<Class<?>> classes() {
        return List.of();
    }

    @Override
    public List<String> types() {
        return List.of(LOT);
    }

    @Override
    public Trace tracer(Object entite) {
        return null;
    }
}
