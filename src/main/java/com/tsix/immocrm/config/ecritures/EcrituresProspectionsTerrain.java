package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Avant les articles : {@code StkItem.LndLandId} référence {@code LndLand}. */
public class EcrituresProspectionsTerrain implements TablesEcrites {

    @Override
    public int rang() {
        return 550;
    }

    @Override
    public List<String> tables() {
        return List.of("LndLand", "LndLandZone");
    }
}
