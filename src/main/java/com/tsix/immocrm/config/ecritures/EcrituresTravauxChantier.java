package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après les référentiels de projet (250-300) et les dossiers (1200) ; types et niveaux avant le jour travaillé. */
public class EcrituresTravauxChantier implements TablesEcrites {

    @Override
    public int rang() {
        return 1215;
    }

    @Override
    public List<String> tables() {
        return List.of("GrhWorkedDayType", "CfgTrancheLevelingDefinition", "GrhWorkedDay", "ComModification");
    }
}
