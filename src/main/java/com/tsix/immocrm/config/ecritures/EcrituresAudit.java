package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Toujours le dernier bloc : la ligne d'audit cite des objets de tous les autres. */
public class EcrituresAudit implements TablesEcrites {

    @Override
    public int rang() {
        return Integer.MAX_VALUE;
    }

    @Override
    public List<String> tables() {
        return List.of("AdmMessage", "AdmWebHistorics");
    }
}
