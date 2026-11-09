package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.prj.PrjProject;

import java.util.List;

/** Écrite comme OMRAN, non rendue par {@code /api/audit} (ouvert à tout authentifié, l'écran est Administrateur). */
public class TracesProjetConfig implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(PrjProject.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof PrjProject p
                ? new Trace("Yes.Prj.Definition.Project.Project", "Projet", p.getDescription(), null, null)
                : null;
    }
}
