package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresEntites implements TablesEcrites {

    @Override
    public int rang() {
        return 200;
    }

    @Override
    public List<String> tables() {
        return List.of("CfgShareholder", "CfgGroupShareholder", "CfgGroupShareholderLine", "CfgCompany",
                "CfgCompanyShareholder");
    }
}
