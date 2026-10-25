package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.stk.StkFSBaseUnit;
import com.tsix.immocrm.entities.stk.StkFSBaseUnitStkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyCfgTranche;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyCfgTrancheStkBaseUnit;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatusHistory;
import com.tsix.immocrm.utils.Champs;

import java.util.List;

import static com.tsix.immocrm.config.audit.RegistreAuditOmran.LIBELLE_TRANCHE;

/** FEATURE-patrimoine-faisabilite.md §2.6 : sans parent ; les deux derniers types, non mesurés en prod, = classe OMRAN. */
public class TracesFaisabilite implements TracesAudit {

    private static final String ETUDE = "Yes.Stk.StkFeasibilityStudy";

    /** Lignes sans parent : seul le type de l'étude elle-même sort de {@code /api/audit?objetId=<étude>}. */
    @Override
    public List<String> types() {
        return List.of(ETUDE);
    }

    @Override
    public List<Class<?>> classes() {
        return List.of(StkFeasibilityStudy.class, StkFSBaseUnitStkFeasibilityStudy.class,
                StkFeasibilityStudyCfgTranche.class, StkFeasibilityStudyCfgTrancheStkBaseUnit.class,
                StkFeasibilityStudyStatusHistory.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return switch (entite) {
            case StkFeasibilityStudy e -> new Trace(ETUDE, "Etude de faisabilité",
                    e.getDescription(), null, null);
            case StkFSBaseUnitStkFeasibilityStudy l -> new Trace("Yes.Stk.BalanceSheet.FSBaseUnitStkFeasibilityStudy",
                    "Prévision", Champs.de(l.getStkFSBaseUnit(), StkFSBaseUnit::getCode), null, null);
            case StkFeasibilityStudyCfgTranche ignore -> new Trace("Yes.Stk.BalanceSheet.FeasibilityStudyCfgTranche",
                    LIBELLE_TRANCHE, LIBELLE_TRANCHE, null, null);
            case StkFeasibilityStudyCfgTrancheStkBaseUnit v -> new Trace(
                    "Yes.Stk.BalanceSheet.FeasibilityStudyCfgTrancheStkBaseUnit", "Unité de la tranche",
                    v.getId().toString(), null, null);
            case StkFeasibilityStudyStatusHistory h -> new Trace("Yes.Stk.Config.FeasibilityStudyStatusHistory",
                    "Historique des états", h.getId().toString(), null, null);
            default -> null;
        };
    }
}
