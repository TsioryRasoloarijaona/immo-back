package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresFichesVerrouillees implements TablesEcrites {

    @Override
    public int rang() {
        return 1760;
    }

    @Override
    public List<String> tables() {
        return List.of("AdmWebLockEntities");
    }
}
