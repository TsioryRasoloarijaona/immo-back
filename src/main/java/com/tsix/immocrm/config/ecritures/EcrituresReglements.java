package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresReglements implements TablesEcrites {

    @Override
    public int rang() {
        return 1300;
    }

    @Override
    public List<String> tables() {
        return List.of("StlSettlement", "StlSettlementLine", "StlQuote", "StlQuoteItem", "StlQuoteDocument");
    }
}
