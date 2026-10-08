package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Filtres de {@link StkItemSpecifications#build(FiltresArticle)} ; null ou vide = pas de restriction.
 * {@code entitesActivesOnly} : jointure interne implicite, un article sans société est écarté, à la différence
 * de {@code VisibiliteEntites} (assumé, {@code FEATURE-visibilite-transversale-entites.md} R2).
 */
public record FiltresArticle(List<UUID> trancheIds, UUID parentId, boolean topLevelOnly, StatutArticle statut,
                             List<UUID> categorieIds, List<UUID> typeIds, String search, boolean entitesActivesOnly,
                             UUID entiteId, UUID natureId, UUID vocationId, UUID etageId,
                             BigDecimal prixDe, BigDecimal prixA, BigDecimal surfaceDe, BigDecimal surfaceA) {
}
