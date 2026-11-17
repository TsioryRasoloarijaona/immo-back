package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.prj.PrjMarketPrevisionModel;

import java.util.List;

/** Modèles de simulation lot ; leurs lignes ne sont pas tracées (FEATURE-lot-previsions-reel.md M7). */
public class TracesModelesSimulation implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(PrjMarketPrevisionModel.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof PrjMarketPrevisionModel m
                ? new Trace("Yes.Prj.Config.MarketPrevisionModel", "Modèle de simulation", m.getCode(), null, null)
                : null;
    }
}
