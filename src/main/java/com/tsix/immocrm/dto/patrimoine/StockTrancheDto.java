package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Stock de la tranche pour « Charger à partir du stock », recalculé comme la vue StkTrancheValorisation.")
public record StockTrancheDto(
        @Schema(description = "Articles de vocation Vente, hors sous-articles (CountItem).") long nombreArticles,
        @Schema(description = "Σ des prix de vente TTC des articles de vocation Vente, sous-articles compris (PriceAllItem).")
        BigDecimal valorisationStock
) {
}
