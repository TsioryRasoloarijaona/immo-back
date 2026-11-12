package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.buy.BuyDocument;

import java.util.List;

/** FEATURE-achat-fiche-ecritures.md E-B7 : type OMRAN par nature, sans parent, libellé = code (frais de terrain compris). */
public class TracesAchat implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(BuyDocument.class);
    }

    @Override
    public Trace tracer(Object entite) {
        if (!(entite instanceof BuyDocument d)) {
            return null;
        }
        return switch (String.valueOf(d.getDocType())) {
            case "QUOTE" -> trace("Yes.Buy.Quote", "Devis", d);
            case "ORDER" -> trace("Yes.Buy.Order", "Bon de Commande", d);
            case "DELIVRY" -> trace("Yes.Buy.Delivery", "Bon de Livraison", d);
            case "REFUND" -> trace("Yes.Stl.Debit.Refund", "Avoir", d);
            default -> trace("Yes.Stl.Debit.Invoice", "Facture", d);
        };
    }

    private static Trace trace(String type, String libelle, BuyDocument d) {
        return new Trace(type, libelle, d.getCode(), null, null);
    }
}
