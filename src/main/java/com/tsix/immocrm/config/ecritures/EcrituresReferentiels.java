package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresReferentiels implements TablesEcrites {

    @Override
    public int rang() {
        return 100;
    }

    @Override
    public List<String> tables() {
        return List.of("StkKind", "StkVocation", "LndVocation", "LndLevel", "CfgTrancheStanding", "StkTrancheHeading",
                "StkBuilding", "StkFloor", "StkOrientation", "StkFacade", "StkVue", "StkCostCategory", "StkContenance",
                "StkItemCategory", "StkItemType", "StkExpenseType");
    }
}
