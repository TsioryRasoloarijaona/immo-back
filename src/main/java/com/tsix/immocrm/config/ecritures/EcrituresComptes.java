package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Avant {@code AdmUserHabilitation} (rang 500), enfant d'{@code AdmUser}. */
public class EcrituresComptes implements TablesEcrites {

    @Override
    public int rang() {
        return 450;
    }

    @Override
    public List<String> tables() {
        return List.of("AdmUser");
    }
}
