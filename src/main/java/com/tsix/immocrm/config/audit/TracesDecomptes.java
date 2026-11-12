package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLine;

import java.util.List;

/** Écrite comme OMRAN, non rendue par {@code /api/audit} (aucun écran d'historique des décomptes). */
public class TracesDecomptes implements TracesAudit {

    private static final String DECOMPTE = "Yes.Prj.WorkingOut.WorkingOut";

    @Override
    public List<Class<?>> classes() {
        return List.of(PrjWorkingOut.class, PrjWorkingOutLine.class);
    }

    @Override
    public Trace tracer(Object entite) {
        if (entite instanceof PrjWorkingOut w) {
            return new Trace(DECOMPTE, "Décompte", w.getCode(), null, null);
        }
        return entite instanceof PrjWorkingOutLine l
                ? new Trace("Yes.Prj.WorkingOut.WorkingOutLine", "Ligne de décompte", l.getDescription(), DECOMPTE,
                l.getPrjWorkingOutId())
                : null;
    }
}
