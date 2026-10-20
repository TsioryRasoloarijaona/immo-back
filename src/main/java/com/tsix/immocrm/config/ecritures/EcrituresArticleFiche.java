package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresArticleFiche implements TablesEcrites {

    @Override
    public int rang() {
        return 800;
    }

    @Override
    public List<String> tables() {
        return List.of("StkItemVue", "StkItemContenance");
    }
}
