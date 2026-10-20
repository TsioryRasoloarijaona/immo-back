package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresApplication implements TablesEcrites {

    @Override
    public int rang() {
        return 1500;
    }

    @Override
    public List<String> tables() {
        return List.of("AppFileObject", "AppNotification", "AppModeleImage", "AppModeleDocument",
                "AppModeleDocumentVersion", "SpecificFunction");
    }
}
