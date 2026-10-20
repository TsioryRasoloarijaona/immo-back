package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.domain.ReferentielAdmin;
import com.tsix.immocrm.entities.stk.StkContenance;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkPresentationTypeContenance;
import com.tsix.immocrm.utils.Champs;

import java.util.List;

/** Les référentiels de {@link ReferentielAdmin} sont tracés par le registre lui-même ; ici, leurs lignes enfants. */
public class TracesReferentiels implements TracesAudit {

    private static final String PRESENTATION = ReferentielAdmin.TYPES_PRESENTATION.audit().type();

    @Override
    public List<Class<?>> classes() {
        return List.of(StkPresentationTypeContenance.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof StkPresentationTypeContenance c
                ? new Trace("Yes.Stk.Config.PresentationTypeContenance", "Contenance",
                        Champs.de(c.getStkContenance(), StkContenance::getDescription), PRESENTATION,
                        Champs.de(c.getStkPresentationType(), StkPresentationType::getId))
                : null;
    }
}
