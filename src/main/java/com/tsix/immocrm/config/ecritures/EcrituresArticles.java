package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresArticles implements TablesEcrites {

    @Override
    public int rang() {
        return 600;
    }

    @Override
    public List<String> tables() {
        return List.of("StkItem", "CfgTrancheStkItem");
    }
}
