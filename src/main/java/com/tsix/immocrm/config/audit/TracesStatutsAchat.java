package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;

import java.util.List;

/** Seuls les statuts sont tracés : OMRAN n'écrit aucune ligne d'historique pour les transitions d'achat. */
public class TracesStatutsAchat implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(BuyDocumentStatus.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof BuyDocumentStatus s
                ? new Trace("Yes.Buy.Config.BuyDocumentStatus", "Statut d'achat", s.getDescription(), null, null)
                : null;
    }
}
