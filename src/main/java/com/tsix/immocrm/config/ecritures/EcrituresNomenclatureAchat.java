package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresNomenclatureAchat implements TablesEcrites {

    @Override
    public int rang() {
        return 1060;
    }

    @Override
    public List<String> tables() {
        return List.of("BuyItemFamily", "BuyItemUnderFamily", "BuyItem");
    }
}
