package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.cfg.CfgGanttProject;

import java.util.List;

/**
 * Planifications Gantt ; OMRAN ne trace ni leurs tâches ni leurs intervenants. Pas de {@code types()} : {@code /api/audit} ne
 * filtre pas le périmètre projet, le journal d'une planification n'est donc pas servi (FEATURE-n9-planification.md N9b).
 */
public class TracesPlanification implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(CfgGanttProject.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof CfgGanttProject p
                ? new Trace("Yes.Cfg.Gantt.CfgGanttProject", "Planification", p.getCode(), null, null)
                : null;
    }
}
