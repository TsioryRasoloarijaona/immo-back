package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresTerrains implements TablesEcrites {

    @Override
    public int rang() {
        return 1100;
    }

    @Override
    public List<String> tables() {
        return List.of("StkExpense", "BuyDocument", "BuyDocumentLine", "BuyVentilationCategory", "BuyDocumentStatusHistory");
    }
}
