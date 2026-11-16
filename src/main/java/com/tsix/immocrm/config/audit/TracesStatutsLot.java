package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.str.StrStatusEcriture;
import com.tsix.immocrm.entities.str.StrWorkFlow;

import java.util.List;

/** Statuts et transitions de lot ; les liens de rôles ne sont pas tracés. */
public class TracesStatutsLot implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(StrStatusEcriture.class, StrWorkFlow.class);
    }

    @Override
    public Trace tracer(Object entite) {
        if (entite instanceof StrStatusEcriture s) {
            return new Trace("Yes.Str.StrStatus", "Statut", s.getCode(), null, null);
        }
        return entite instanceof StrWorkFlow w ? new Trace("Yes.Str.StrWorkFlow", "Transition", w.getCode(), null, null) : null;
    }
}
