package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.entities.cfg.LigneActionnaireOmran;
import com.tsix.immocrm.utils.Champs;

import java.util.List;

import static com.tsix.immocrm.config.audit.RegistreAuditOmran.ACTIONNAIRE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.ENTITE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.GROUPE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.LIGNE_GROUPE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.REPARTITION;

public class TracesEntites implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(CfgCompany.class, CfgCompanyShareholder.class, CfgShareholder.class, CfgGroupShareholder.class,
                CfgGroupShareholderLine.class);
    }

    @Override
    public List<String> types() {
        return List.of(ENTITE, REPARTITION, ACTIONNAIRE, GROUPE, LIGNE_GROUPE);
    }

    @Override
    public Trace tracer(Object entite) {
        return switch (entite) {
            case CfgCompany c -> new Trace(ENTITE, "Entité juridique", c.getCode(), null, null);
            case CfgCompanyShareholder r -> new Trace(REPARTITION, "Répartition", codeActionnaire(r), ENTITE,
                    Champs.de(r.getCfgCompany(), CfgCompany::getId));
            case CfgShareholder a -> new Trace(ACTIONNAIRE, "Actionnaire", a.getCode(), null, null);
            case CfgGroupShareholder g -> new Trace(GROUPE, "Groupe d'actionnaires", g.getCode(), null, null);
            case CfgGroupShareholderLine l -> new Trace(LIGNE_GROUPE, "Ligne de groupe", codeActionnaire(l), GROUPE,
                    Champs.de(l.getCfgGroupShareholder(), CfgGroupShareholder::getId));
            default -> null;
        };
    }

    private static String codeActionnaire(LigneActionnaireOmran ligne) {
        return Champs.de(ligne.getCfgShareholder(), CfgShareholder::getCode);
    }
}
