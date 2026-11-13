package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après les projets et types de lot (250) qu'un lot cite (FEATURE-lots-liste-parite.md E8). */
public class EcrituresLots implements TablesEcrites {

    @Override
    public int rang() {
        return 260;
    }

    @Override
    public List<String> tables() {
        return List.of("PrjMarket", "PrjMarketLineGrouping", "PrjMarketLineGroupingEstimation", "PrjMarketLine",
                "PrjMarketLineEstimation", "PrjMarketVentilationCategory");
    }
}
