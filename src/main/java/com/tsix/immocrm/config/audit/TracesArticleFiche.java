package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.stk.StkContenance;
import com.tsix.immocrm.entities.stk.StkItemContenance;
import com.tsix.immocrm.entities.stk.StkItemVue;
import com.tsix.immocrm.entities.stk.StkVue;
import com.tsix.immocrm.utils.Champs;

import java.util.List;

import static com.tsix.immocrm.config.audit.RegistreAuditOmran.CONTENANCE_ARTICLE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.VUE_ARTICLE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.ligneArticle;

public class TracesArticleFiche implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(StkItemVue.class, StkItemContenance.class);
    }

    @Override
    public List<String> types() {
        return List.of(VUE_ARTICLE, CONTENANCE_ARTICLE);
    }

    @Override
    public Trace tracer(Object entite) {
        return switch (entite) {
            case StkItemVue v -> ligneArticle(VUE_ARTICLE, "Vues", Champs.de(v.getStkVue(), StkVue::getDescription),
                    v.getStkItem());
            case StkItemContenance c -> ligneArticle(CONTENANCE_ARTICLE, "Contenances",
                    Champs.de(c.getStkContenance(), StkContenance::getDescription), c.getStkItem());
            default -> null;
        };
    }
}
