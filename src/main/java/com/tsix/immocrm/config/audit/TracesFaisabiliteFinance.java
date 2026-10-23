package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.stk.FSFunding;
import com.tsix.immocrm.entities.stk.FSPrevision;
import com.tsix.immocrm.entities.stk.FSReimbursement;

import java.util.List;

/**
 * FEATURE-patrimoine-faisabilite-workflow.md §3 et décision 9 : crédits, remboursements et lignes saisies du bilan ;
 * aucun type mesuré en prod = classe OMRAN. Les lignes dérivées (par tranche, trésorerie) ne sont pas tracées.
 */
public class TracesFaisabiliteFinance implements TracesAudit {

    private static final String ETUDE = "Yes.Stk.StkFeasibilityStudy";

    @Override
    public List<Class<?>> classes() {
        return List.of(FSFunding.class, FSReimbursement.class, FSPrevision.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return switch (entite) {
            case FSFunding f -> new Trace("Yes.Stk.FSFunding", "Déblocage de crédit", f.getDescription(), ETUDE,
                    f.getFeasibilityStudyId());
            case FSReimbursement r -> new Trace("Yes.Stk.FSReimbursement", "Remboursement de crédit", r.getId().toString(),
                    "Yes.Stk.FSFunding", r.getFsFundingId());
            case FSPrevision p -> new Trace("Yes.Stk.BalanceSheet.FSPrevision", "Prévision", String.valueOf(p.getStlCategoryId()),
                    ETUDE, p.getFeasibilityStudyId());
            default -> null;
        };
    }
}
