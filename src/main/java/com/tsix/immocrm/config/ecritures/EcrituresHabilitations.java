package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresHabilitations implements TablesEcrites {

    @Override
    public int rang() {
        return 500;
    }

    @Override
    public List<String> tables() {
        return List.of("AdmUserHabilitation");
    }
}
