package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/tranches/{id}/articles}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code StkItem}. Adjust once a spec is available.
 */
public record ArticleListItemDto(
        UUID id,
        String code,
        String description,
        BigDecimal area,
        BigDecimal salePrice,
        BigDecimal comSalePrice,
        StatutArticle statut,
        String titreFoncier,
        String categorie,
        String type
) {
}
