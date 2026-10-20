package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresTrancheOnglets implements TablesEcrites {

    @Override
    public int rang() {
        return 700;
    }

    @Override
    public List<String> tables() {
        return List.of("CfgTrancheLevel", "CfgTrancheCoveredFloor", "CfgTrancheSettlementCategory",
                "CfgTrancheFolderEntity", "CfgPrevision");
    }
}
