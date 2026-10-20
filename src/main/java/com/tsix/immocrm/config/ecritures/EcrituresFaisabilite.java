package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

public class EcrituresFaisabilite implements TablesEcrites {

    @Override
    public int rang() {
        return 1600;
    }

    @Override
    public List<String> tables() {
        return List.of("StkFSBaseUnit", "StkFeasibilityStudy", "StkFeasibilityStudyStatusHistory",
                "StkFSBaseUnitStkFeasibilityStudy", "StkFeasibilityStudyCfgTranche",
                "StkFeasibilityStudyCfgTrancheStkBaseUnit");
    }
}
