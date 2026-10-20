package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresTiers implements TablesEcrites {

    @Override
    public int rang() {
        return 1000;
    }

    @Override
    public List<String> tables() {
        return List.of("CfgTier", "ComAction");
    }
}
