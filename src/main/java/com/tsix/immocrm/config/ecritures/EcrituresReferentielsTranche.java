package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresReferentielsTranche implements TablesEcrites {

    @Override
    public int rang() {
        return 400;
    }

    @Override
    public List<String> tables() {
        return List.of("StkPresentationType", "StkPresentationTypeContenance", "StkItemExonaration", "CfgTrancheStatus");
    }
}
