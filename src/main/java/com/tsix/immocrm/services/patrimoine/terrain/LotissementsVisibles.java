package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.ArticleService.VOCATION_PROJET;

/**
 * Seule recherche par id des routes {@code /api/lotissements/{id}…} : terrain d'une tranche visible, d'entité « article »,
 * vocation autre que PROJET (FEATURE-patrimoine-lotissements.md §1.1) ; sinon 404.
 */
@Component
public record LotissementsVisibles(StkItemRepository items, StrEntityRepository entites) {

    static final String ENTITE_ARTICLE = "StkItem";

    public StkItem exiger(UUID id) {
        return items.findVisible(id).filter(this::estLot)
                .orElseThrow(() -> new ResourceNotFoundException("Lot introuvable: " + id));
    }

    public boolean estLot(StkItem item) {
        return TerrainsVisibles.estTerrain(item) && item.getStkHierarchy() != null
                && !Boolean.TRUE.equals(item.getIsUnderItem())
                && !VOCATION_PROJET.equals(Champs.de(item.getStkVocation(), StkVocation::getCode))
                && entites.findByCodeIn(List.of(ENTITE_ARTICLE)).stream().map(StrEntity::getId)
                .anyMatch(e -> e.equals(item.getStrEntityId()));
    }
}
