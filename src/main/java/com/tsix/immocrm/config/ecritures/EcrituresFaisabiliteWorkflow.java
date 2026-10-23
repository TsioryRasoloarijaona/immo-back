package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Statuts et transitions : parents des études et de leur historique (rang 1600), donc avant. */
public class EcrituresFaisabiliteWorkflow implements TablesEcrites {

    @Override
    public int rang() {
        return 1550;
    }

    @Override
    public List<String> tables() {
        return List.of("StkFeasibilityStudyStatus", "StkFeasibilityStudyStatusDocumentToAttach", "StkFeasibilityStudyWorkFlow",
                "StkFeasibilityStudyWorkFlowAdmRole", "StkFeasibilityStudyWorkFlowDocumentToAttach");
    }
}
