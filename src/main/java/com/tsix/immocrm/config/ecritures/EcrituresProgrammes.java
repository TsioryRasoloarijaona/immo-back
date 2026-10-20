package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresProgrammes implements TablesEcrites {

    @Override
    public int rang() {
        return 300;
    }

    @Override
    public List<String> tables() {
        return List.of("CfgProject", "CfgProjectZone", "CfgTranche");
    }
}
