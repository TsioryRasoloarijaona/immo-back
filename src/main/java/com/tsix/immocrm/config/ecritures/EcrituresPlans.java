package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresPlans implements TablesEcrites {

    @Override
    public int rang() {
        return 900;
    }

    @Override
    public List<String> tables() {
        return List.of("StkHierarchyPath", "StkHierarchyPlan");
    }
}
