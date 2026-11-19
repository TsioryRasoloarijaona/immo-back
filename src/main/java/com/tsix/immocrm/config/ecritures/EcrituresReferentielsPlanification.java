package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Avant les lots (260) dont les réceptions citent types et natures (FEATURE-n9-planification.md N9a). */
public class EcrituresReferentielsPlanification implements TablesEcrites {

    @Override
    public int rang() {
        return 253;
    }

    @Override
    public List<String> tables() {
        return List.of("PrjMarketReceptionType", "PrjMarketReceptionKind", "CfgGanttTaskType");
    }
}
