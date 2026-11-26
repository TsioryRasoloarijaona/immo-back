package com.tsix.immocrm.services.perimetre;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Seule règle « article visible » d'un compte agence (FEATURE-agences-vente.md V1, R-V4, R-V6) : tranche d'un projet
 * affecté et (article libre ou dossier d'un vendeur de l'agence). Sans effet pour un compte du siège.
 */
@Component
public record ArticlesVisibles(PerimetreCommercialService perimetre, StkItemRepository items,
                               TrancheVisibleRepository tranches) {

    /** À ajouter par {@code and} aux filtres de liste, d'export et de sélection (R-V7). */
    public Specification<StkItem> filtre() {
        return restriction().orElse((root, query, cb) -> cb.conjunction());
    }

    /** Lecture par id (fiche, sous-ressources, pièces jointes) : même règle que {@link #filtre()}, sinon vide. */
    public Optional<StkItem> trouver(UUID id) {
        Optional<Specification<StkItem>> restriction = restriction();
        return items.findVisible(id).filter(article -> restriction
                .map(r -> items.exists(r.and((root, query, cb) -> cb.equal(root.get("id"), id))))
                .orElse(true));
    }

    /** Tranche visible (R4) ; pour une agence, aussi dans ses projets, sinon 404. */
    public void exigerTranche(UUID trancheId) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        if (perimetre.vendeursAgenceCourante().isPresent()
                && !perimetre.projetVisible().test(Champs.de(tranche, CfgTranche::getCfgProject, CfgProject::getId))) {
            throw new ResourceNotFoundException("Tranche introuvable: " + trancheId);
        }
    }

    private Optional<Specification<StkItem>> restriction() {
        return perimetre.vendeursAgenceCourante().map(vendeurs -> {
            List<UUID> projets = perimetre.perimetreCourant().orElseGet(() -> List.of(AgenceDuCompte.AUCUN));
            return (root, query, cb) -> {
                // StkItem.ComFolderId n'est pas une association JPA : sous-requête sur ComFolder.
                Subquery<UUID> deLAgence = query.subquery(UUID.class);
                Root<ComFolder> dossier = deLAgence.from(ComFolder.class);
                deLAgence.select(dossier.get("id")).where(cb.equal(dossier.get("id"), root.get("comFolderId")),
                        dossier.get("cfgCommercial").get("id").in(vendeurs));
                return cb.and(root.get("stkHierarchy").get("cfgProject").get("id").in(projets),
                        cb.or(cb.isNull(root.get("comFolderId")), cb.exists(deLAgence)));
            };
        });
    }
}
