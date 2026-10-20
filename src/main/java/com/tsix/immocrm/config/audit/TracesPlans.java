package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.stk.StkHierarchyPlan;

import java.util.List;

public class TracesPlans implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(StkHierarchyPlan.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof StkHierarchyPlan p ? new Trace("Yes.Cfg.StkHierarchyPlan", "Plan", p.getCode(), null, null) : null;
    }
}
