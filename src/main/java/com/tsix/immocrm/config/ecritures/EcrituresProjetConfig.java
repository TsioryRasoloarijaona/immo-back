package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après les sociétés (200), avant les habilitations (500) qui citent {@code PrjProject} (FEATURE-projet-config.md). */
public class EcrituresProjetConfig implements TablesEcrites {

    @Override
    public int rang() {
        return 250;
    }

    @Override
    public List<String> tables() {
        return List.of("PrjMarketType", "PrjProject", "PrjProjectCfgTrancheVentilation", "PrjProjectProgressCriteriaLine");
    }
}
