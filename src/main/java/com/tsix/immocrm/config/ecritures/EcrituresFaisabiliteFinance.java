package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après l'étude et ses grilles (rang 1600) : crédit avant remboursement, lignes dérivées après les tranches d'étude. */
public class EcrituresFaisabiliteFinance implements TablesEcrites {

    @Override
    public int rang() {
        return 1610;
    }

    @Override
    public List<String> tables() {
        return List.of("FSFunding", "FSReimbursement", "FSPrevision", "StkFSPrevisionStkTranche", "StkFSPrevisionLine");
    }
}
