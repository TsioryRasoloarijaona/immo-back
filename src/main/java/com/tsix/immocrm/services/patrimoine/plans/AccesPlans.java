package com.tsix.immocrm.services.patrimoine.plans;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkHierarchyPlan;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.stk.StkHierarchyPlanRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Seule règle d'accès des routes de plans (FEATURE-patrimoine-plans.md, réponse 1) : tranche visible (R4) et projet
 * dans le périmètre commercial, sinon 404. Un projet absent reste visible, comme dans {@link PerimetreCommercialService}.
 */
@Component
record AccesPlans(TrancheVisibleRepository tranches, StkHierarchyPlanRepository plans, StkItemRepository articles,
                  PerimetreCommercialService perimetre) {

    CfgTranche tranche(UUID trancheId) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        if (!dansPerimetre(perimetre.perimetreCourant(), projet(tranche))) {
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
        return articles.findVisible(articleId)
                .filter(a -> dansPerimetre(perimetre.perimetreCourant(), projet(a.getStkHierarchy())))
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable: " + articleId));
    }

    /** Vide = non restreint (rôle exempté, aucun projet affecté). */
    static boolean dansPerimetre(Optional<List<UUID>> perimetre, UUID projetId) {
        return projetId == null || perimetre.map(ids -> ids.contains(projetId)).orElse(true);
    }

    private static UUID projet(CfgTranche tranche) {
        return Champs.de(tranche, CfgTranche::getCfgProject, CfgProject::getId);
    }
}
