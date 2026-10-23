package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatus;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatusDocumentToAttach;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyWorkFlow;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyWorkFlowAdmRole;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyWorkFlowDocumentToAttach;

import java.util.List;

/** FEATURE-patrimoine-faisabilite-workflow.md §5 : types non mesurés en prod = classe OMRAN. */
public class TracesFaisabiliteWorkflow implements TracesAudit {

    private static final String DOCUMENT = "Document attaché";

    @Override
    public List<Class<?>> classes() {
        return List.of(StkFeasibilityStudyStatus.class, StkFeasibilityStudyWorkFlow.class,
                StkFeasibilityStudyWorkFlowAdmRole.class, StkFeasibilityStudyWorkFlowDocumentToAttach.class,
                StkFeasibilityStudyStatusDocumentToAttach.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return switch (entite) {
            case StkFeasibilityStudyStatus s -> new Trace("Yes.Stk.Config.FeasibilityStudyStatus",
                    "Statut étude de faisabilité", s.getCode(), null, null);
            case StkFeasibilityStudyWorkFlow w -> new Trace("Yes.Stk.Config.FeasibilityStudyWorkFlow",
                    "Transition étude de faisabilité", w.getDescription(), null, null);
            case StkFeasibilityStudyWorkFlowAdmRole r -> new Trace("Yes.Stk.Config.FeasibilityStudyWorkFlowAdmRole",
                    "Rôle de transition", r.getId().toString(), null, null);
            case StkFeasibilityStudyWorkFlowDocumentToAttach d -> new Trace(
                    "Yes.Stk.Config.FeasibilityStudyWorkFlowDocumentToAttach", DOCUMENT, d.getId().toString(), null, null);
            case StkFeasibilityStudyStatusDocumentToAttach d -> new Trace(
                    "Yes.Stk.Config.FeasibilityStudyStatusDocumentToAttach", DOCUMENT, d.getId().toString(), null, null);
            default -> null;
        };
    }
}
