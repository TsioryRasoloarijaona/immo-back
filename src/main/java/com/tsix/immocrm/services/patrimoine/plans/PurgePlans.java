package com.tsix.immocrm.services.patrimoine.plans;

import com.tsix.immocrm.repositories.stk.StkHierarchyPathRepository;
import com.tsix.immocrm.repositories.stk.StkHierarchyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** « Puger les plans » : tracés des articles de la tranche puis ses plans, par entités (audit, recopie). */
@Service
public class PurgePlans {

    private final AccesPlans acces;
    private final StkHierarchyPlanRepository plans;
    private final StkHierarchyPathRepository traces;

    public PurgePlans(AccesPlans acces, StkHierarchyPlanRepository plans, StkHierarchyPathRepository traces) {
        this.acces = acces;
        this.plans = plans;
        this.traces = traces;
    }

    @Transactional
    public void purger(UUID trancheId) {
        acces.tranche(trancheId);
        traces.deleteAll(traces.tracesDeLaTranche(trancheId));
        plans.deleteAll(plans.findByStkHierarchyId(trancheId));
    }
}
