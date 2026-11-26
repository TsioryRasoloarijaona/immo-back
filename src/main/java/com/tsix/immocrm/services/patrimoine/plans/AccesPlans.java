package com.tsix.immocrm.services.patrimoine.plans;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkHierarchyPlan;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.stk.StkHierarchyPlanRepository;
import com.tsix.immocrm.services.perimetre.ArticlesVisibles;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seule règle d'accès des routes de plans (FEATURE-patrimoine-plans.md, réponse 1) : tranche visible (R4) et projet
 * visible ({@link PerimetreCommercialService#projetVisible()}), sinon 404.
 */
@Component
record AccesPlans(TrancheVisibleRepository tranches, StkHierarchyPlanRepository plans, ArticlesVisibles articles,
                  PerimetreCommercialService perimetre) {

    CfgTranche tranche(UUID trancheId) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        if (!perimetre.projetVisible().test(projet(tranche))) {
            throw new ResourceNotFoundException("Tranche introuvable: " + trancheId);
        }
        return tranche;
    }

    StkHierarchyPlan plan(UUID planId) {
        StkHierarchyPlan plan = plans.findById(planId).filter(p -> p.getStkHierarchyId() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Plan introuvable: " + planId));
        tranche(plan.getStkHierarchyId());
        return plan;
    }

    StkItem article(UUID articleId) {
        return articles.trouver(articleId)
                .filter(a -> perimetre.projetVisible().test(projet(a.getStkHierarchy())))
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable: " + articleId));
    }

    private static UUID projet(CfgTranche tranche) {
        return Champs.de(tranche, CfgTranche::getCfgProject, CfgProject::getId);
    }
}
