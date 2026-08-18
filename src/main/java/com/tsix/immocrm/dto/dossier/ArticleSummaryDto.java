package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Article summary embedded in {@link DossierDetailDto#articles()}.
 *
 * NOTE (confirmed): a Dossier can have several articles — e.g. real dossier
 * "MBR 2022/00205" links both "MB 18 1 04" (main lot) and "MB 18 1 04 TER" (its annex) —
 * so this is a list, never a single article.
 */
public record ArticleSummaryDto(
        UUID id,
        String code,
        String description,
        BigDecimal area,
        BigDecimal price
) {
}
