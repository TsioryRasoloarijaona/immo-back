package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.lnd.LndLand;

import java.util.List;

/** FEATURE-patrimoine-prospections-terrain.md §6.2 : libellé = Description ; zones non historisées (comme OMRAN). */
public class TracesProspectionsTerrain implements TracesAudit {

    private static final String TYPE = "Yes.Lnd.Prospection";

    @Override
    public List<Class<?>> classes() {
        return List.of(LndLand.class);
    }

    @Override
    public List<String> types() {
        return List.of(TYPE);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof LndLand p ? new Trace(TYPE, "Prospection terrain", p.getDescription(), null, null) : null;
    }
}
