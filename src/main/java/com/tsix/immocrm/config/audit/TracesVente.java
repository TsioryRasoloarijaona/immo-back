package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.com.ComSaleWithdrawalCancellation;

import java.util.List;

public class TracesVente implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(ComSaleWithdrawalCancellation.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof ComSaleWithdrawalCancellation w
                ? new Trace("Yes.Com.Sales.SaleWithdrawalCancellation", "Désistement", w.getCode(), null, null)
                : null;
    }
}
