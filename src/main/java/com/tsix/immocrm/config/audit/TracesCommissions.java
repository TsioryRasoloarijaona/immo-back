package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.com.ComCommissionLine;

import java.util.List;

/** FEATURE-vente-commissions.md §3.3 : ligne de commission sans parent, libellés lus en prod. */
public class TracesCommissions implements TracesAudit {

    static final String LIGNE = "Yes.Com.Commission.CommissionLine";
    private static final String LIBELLE = "CommissionLine";

    @Override
    public List<Class<?>> classes() {
        return List.of(ComCommissionLine.class);
    }

    @Override
    public List<String> types() {
        return List.of(LIGNE);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof ComCommissionLine ? new Trace(LIGNE, LIBELLE, LIBELLE, null, null) : null;
    }
}
