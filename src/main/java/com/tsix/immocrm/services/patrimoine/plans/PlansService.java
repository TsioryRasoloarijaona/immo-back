package com.tsix.immocrm.services.patrimoine.plans;

import com.tsix.immocrm.dto.patrimoine.plans.PlanArticleDto;
import com.tsix.immocrm.dto.patrimoine.plans.PlanDto;
import com.tsix.immocrm.dto.patrimoine.plans.TraceDto;
import com.tsix.immocrm.dto.patrimoine.plans.TrancheAvecPlansDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkHierarchyPlan;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkHierarchyPathRepository;
import com.tsix.immocrm.repositories.stk.StkHierarchyPathRepository.TraceArticle;
import com.tsix.immocrm.repositories.stk.StkHierarchyPlanRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lectures des plans (FEATURE-patrimoine-plans.md §2) ; toutes passent par {@link AccesPlans}. */
@Service
@Transactional(readOnly = true)
public class PlansService {

    private final AccesPlans acces;
    private final StkHierarchyPlanRepository plans;
    private final StkHierarchyPathRepository traces;
    private final ComFolderRepository dossiers;
    private final PerimetreCommercialService perimetre;

    public PlansService(AccesPlans acces, StkHierarchyPlanRepository plans, StkHierarchyPathRepository traces,
                        ComFolderRepository dossiers, PerimetreCommercialService perimetre) {
        this.acces = acces;
        this.plans = plans;
        this.traces = traces;
        this.dossiers = dossiers;
        this.perimetre = perimetre;
    }

    public List<TrancheAvecPlansDto> tranchesAvecPlans() {
        Optional<List<UUID>> projets = perimetre.perimetreCourant();
        return plans.tranchesVisiblesAvecPlans().stream()
                .filter(l -> AccesPlans.dansPerimetre(projets, (UUID) l[3]))
                .map(l -> new TrancheAvecPlansDto((UUID) l[0], (String) l[1], (String) l[2], (String) l[4],
                        ((Number) l[5]).longValue()))
                .toList();
    }

    public List<PlanDto> plansDeLaTranche(UUID trancheId) {
        acces.tranche(trancheId);
        return plans.plansDeLaTranche(trancheId);
    }

    public String etagImage(UUID planId) {
        acces.plan(planId);
        return plans.empreinteImage(planId).orElseThrow(() -> imageIntrouvable(planId));
    }

    public ImageDataUri image(UUID planId) {
        acces.plan(planId);
        return plans.image(planId).flatMap(ImageDataUri::lire).orElseThrow(() -> imageIntrouvable(planId));
    }

    public List<TraceDto> traces(UUID planId) {
        StkHierarchyPlan plan = acces.plan(planId);
        List<TraceArticle> lignes = traces.tracesDuPlan(plan.getStkFloorId(), plan.getStkHierarchyId());
        Map<UUID, ComFolder> occupants = occupants(lignes.stream().map(TraceArticle::getArticleId).distinct().toList());
        return lignes.stream().map(l -> trace(l, occupants.get(l.getArticleId()))).toList();
    }

    public List<PlanArticleDto> plansDeLArticle(UUID articleId) {
        StkItem article = acces.article(articleId);
        UUID trancheId = Champs.de(article.getStkHierarchy(), CfgTranche::getId);
        return trancheId == null ? List.of() : plans.plansDeLArticle(trancheId, articleId);
    }

    /** Occupant = dossier de l'article dont le statut est actif ({@link StrStatus#estActif()}) ; statut absent = actif. */
    private Map<UUID, ComFolder> occupants(List<UUID> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return dossiers.dossiersDesArticles(articleIds).stream()
                .filter(f -> f.getStrStatus() == null || f.getStrStatus().estActif())
                .collect(Collectors.toMap(f -> f.getStkItem().getId(), Function.identity(), (a, b) -> a));
    }

    private static TraceDto trace(TraceArticle ligne, ComFolder occupant) {
        StrStatus statut = Champs.de(occupant, ComFolder::getStrStatus);
        return new TraceDto(ligne.getArticleId(), ligne.getCode(), ligne.getD(),
                Champs.de(statut, StrStatus::getCode), Champs.de(statut, StrStatus::getDescription),
                Champs.de(statut, StrStatus::getColorName), Champs.de(occupant, ComFolder::getId),
                occupant == null ? null : TierNomFormatter.nom(occupant.getCfgCustomer()),
                Boolean.TRUE.equals(ligne.getSousArticle()), occupant == null);
    }

    private static ResourceNotFoundException imageIntrouvable(UUID planId) {
        return new ResourceNotFoundException("Image du plan absente ou illisible: " + planId);
    }
}
