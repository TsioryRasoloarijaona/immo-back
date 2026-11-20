package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.buy.BuySelection;

import java.util.List;

/** Demandes internes d'achat (FEATURE-n6-demandes-achat.md) ; les procédures sont tracées par le registre des référentiels. */
public class TracesDemandesAchat implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(BuySelection.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof BuySelection d
                ? new Trace("Yes.Buy.CallOfTender.BuySelection", "Demande interne d'achat", d.getCode(), null, null)
                : null;
    }
}
