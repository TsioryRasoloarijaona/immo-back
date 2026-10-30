package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresTaches implements TablesEcrites {

    @Override
    public int rang() {
        return 1720;
    }

    @Override
    public List<String> tables() {
        return List.of("AdmTaskType", "AdmTaskPriority", "AdmTask");
    }
}
