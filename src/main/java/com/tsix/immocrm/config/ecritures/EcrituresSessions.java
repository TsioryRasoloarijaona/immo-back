package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresSessions implements TablesEcrites {

    @Override
    public int rang() {
        return 1750;
    }

    @Override
    public List<String> tables() {
        return List.of("AdmWebSessions");
    }
}
