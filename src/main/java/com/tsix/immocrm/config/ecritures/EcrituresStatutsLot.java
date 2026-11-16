package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Avant les lots (260) qui citent {@code StrStatus} ; {@code CfgImage} vient d'ici, avant le statut qui la cite. */
public class EcrituresStatutsLot implements TablesEcrites {

    @Override
    public int rang() {
        return 255;
    }

    @Override
    public List<String> tables() {
        return List.of("CfgImage", "StrStatus", "StrStatusMandatoryField", "StrWorkFlow", "StrIntervener",
                "StrWorkFlowIntervener");
    }
}
