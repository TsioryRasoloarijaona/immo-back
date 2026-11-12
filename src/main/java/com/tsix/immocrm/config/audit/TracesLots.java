package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.prj.PrjMarket;

import java.util.List;

/** Lot créé, supprimé ou changé de statut (FEATURE-lots-liste-parite.md E5–E7), objet OMRAN de la fiche lot. */
public class TracesLots implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(PrjMarket.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof PrjMarket m ? new Trace("Yes.Prj.Market.MarketMonitoring", "Lot", m.getCode(), null, null)
                : null;
    }
}
