package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.utils.Champs;

import java.util.List;
import java.util.Objects;

import static com.tsix.immocrm.config.audit.RegistreAuditOmran.ARTICLE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.LIBELLE_TRANCHE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.PROGRAMME;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.SOUS_ARTICLE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.TERRAIN;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.TRANCHE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.TRANCHE_ARTICLE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.article;

public class TracesProgrammes implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(CfgProject.class, CfgTranche.class, StkItem.class, CfgTrancheStkItem.class);
    }

    @Override
    public List<String> types() {
        return List.of(PROGRAMME, TRANCHE, TRANCHE_ARTICLE, ARTICLE, SOUS_ARTICLE, TERRAIN);
    }

    /** ObjectLabel : programme = Code ; tranche = Description (mesuré en prod). */
    @Override
    public Trace tracer(Object entite) {
        return switch (entite) {
            case CfgProject p -> new Trace(PROGRAMME, "Programme immobilier", p.getCode(), null, null);
            case CfgTranche t -> new Trace(TRANCHE, LIBELLE_TRANCHE, t.getDescription(), null, null);
            case StkItem i -> article(i);
            case CfgTrancheStkItem l -> new Trace(TRANCHE_ARTICLE, LIBELLE_TRANCHE, Objects.toString(l.getSorting(), ""),
                    TRANCHE, Champs.de(l.getCfgTranche(), CfgTranche::getId));
            default -> null;
        };
    }
}
