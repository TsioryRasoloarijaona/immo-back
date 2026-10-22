package com.tsix.immocrm.services.rapport;

import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tranches qu'un rapport a le droit de lire (FEATURE-tdb-vente-rapports-socle.md §3.4) : société active (même clause
 * que {@code TrancheVisibleRepository.TRANCHE_VISIBLE}), périmètre commercial (projet NULL visible), en vente si le
 * module l'exige, et parmi les tranches demandées s'il y en a.
 */
@Component
public class TranchesVisiblesRapport {

    private static final String VISIBLES = "SELECT t FROM CfgTranche t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc WHERE " + VisibiliteEntites.CLAUSE_JPQL + " ORDER BY t.description";

    private final EntityManager em;
    private final PerimetreCommercialService perimetreService;

    public TranchesVisiblesRapport(EntityManager em, PerimetreCommercialService perimetreService) {
        this.em = em;
        this.perimetreService = perimetreService;
    }

    /** Triées par libellé : options du paramètre {@code TRANCHES}. */
    public List<CfgTranche> visibles(ModuleRapport module) {
        Optional<Set<UUID>> perimetre = perimetreService.perimetreCourant().map(Set::copyOf);
        return em.createQuery(VISIBLES, CfgTranche.class).getResultList().stream()
                .filter(t -> !module.tranchesEnVenteSeulement() || Boolean.TRUE.equals(t.getIsForSale()))
                .filter(t -> perimetre.isEmpty() || t.getCfgProject() == null
                        || perimetre.get().contains(t.getCfgProject().getId()))
                .toList();
    }

    /** Une demande sans aucune tranche visible donne une liste vide, jamais « toutes » (RV1). */
    public List<UUID> retenues(ModuleRapport module, Collection<UUID> demandees) {
        List<UUID> visibles = visibles(module).stream().map(CfgTranche::getId).toList();
        if (demandees.isEmpty()) {
            return visibles;
        }
        Set<UUID> voulues = Set.copyOf(demandees);
        return visibles.stream().filter(voulues::contains).toList();
    }
}
