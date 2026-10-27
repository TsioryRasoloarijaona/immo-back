package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresCommissions implements TablesEcrites {

    @Override
    public int rang() {
        return 1710;
    }

    @Override
    public List<String> tables() {
        return List.of("ComCommissionLine", "BuyVentilationExpense");
    }
}
