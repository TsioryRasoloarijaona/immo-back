package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Detail shape for {@code GET /api/articles/{id}}, including sub-lots (via {@code lotParentId}
 * / {@code StkItem.parent}) — e.g. a cave/parking annexed to a main lot.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code StkItem}. Adjust once a spec is available.
 */
public record ArticleDetailDto(
        UUID id,
        String code,
        String description,
        String adresse,
        BigDecimal area,
        BigDecimal salePrice,
        BigDecimal comSalePrice,
        StatutArticle statut,
        UUID trancheId,
        UUID lotParentId,
        List<ArticleListItemDto> sousLots,
        String titreFoncier,
        String categorie,
        String type
) {
}
