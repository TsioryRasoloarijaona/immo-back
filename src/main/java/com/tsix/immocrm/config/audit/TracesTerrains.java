package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.stk.StkExpense;
import com.tsix.immocrm.entities.stk.StkExpenseType;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.utils.Champs;

import java.util.List;

import static com.tsix.immocrm.config.audit.RegistreAuditOmran.TERRAIN;

/** FEATURE-patrimoine-terrains.md §3.2 : frais sous son terrain ; facture sans parent, libellée par son code. */
public class TracesTerrains implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(StkExpense.class, BuyDocument.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return switch (entite) {
            case StkExpense e -> new Trace("Yes.Stk.Expense.Expense", "Frais de Terrain",
                    Champs.de(e.getStkExpenseType(), StkExpenseType::getDescription), TERRAIN,
                    Champs.de(e.getStkItem(), StkItem::getId));
            case BuyDocument f -> new Trace("Yes.Stl.Debit.Invoice", "Facture", f.getCode(), null, null);
            default -> null;
        };
    }
}
