package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.grh.GrhWorkedDay;

import java.util.List;

/** Écrite comme les autres {@code Yes.Grh.*}, non rendue : l'audit visuel de la fiche est hors lot (D9). */
public class TracesTravauxChantier implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(GrhWorkedDay.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof GrhWorkedDay j
                ? new Trace("Yes.Grh.GrhWorkedDay", "Jour travaillé", j.getCode(), null, null)
                : null;
    }
}
