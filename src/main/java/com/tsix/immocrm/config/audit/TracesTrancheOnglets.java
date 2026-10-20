package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.cfg.CfgTrancheFolderEntity;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.cfg.ComSalesCommission;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.utils.Champs;

import java.util.List;
import java.util.Objects;

import static com.tsix.immocrm.config.audit.RegistreAuditOmran.TRANCHE;
import static com.tsix.immocrm.config.audit.RegistreAuditOmran.TRANCHE_ETAGE;

public class TracesTrancheOnglets implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(CfgTrancheLevel.class, CfgTrancheCoveredFloor.class, CfgTrancheSettlementCategory.class,
                AdmUserHabilitation.class, ComSalesCommission.class, CfgTrancheFolderEntity.class, ComDocument.class);
    }

    @Override
    public List<String> types() {
        return List.of(TRANCHE_ETAGE);
    }

    @Override
    public Trace tracer(Object entite) {
        return switch (entite) {
            // Libellés d'objet mesurés en prod : PKey de la ligne, Sorting, AdmUserId ; sans parent sauf l'étage (§8.10).
            case CfgTrancheLevel l -> new Trace(TRANCHE_ETAGE, "Etage", l.getId().toString(), TRANCHE,
                    Champs.de(l.getCfgTranche(), CfgTranche::getId));
            case CfgTrancheCoveredFloor p -> new Trace("Yes.Cfg.CfgTrancheCoveredFloor", "Prospection de projet",
                    p.getId().toString(), null, null);
            case CfgTrancheSettlementCategory f -> new Trace("Yes.Cfg.CfgTrancheSettlementCategory", "Frais de vente",
                    Objects.toString(f.getSorting(), ""), null, null);
            case AdmUserHabilitation h -> new Trace("Yes.Adm.AdmUserHabilitation", "Habilitation",
                    Objects.toString(Champs.de(h.getAdmUser(), AdmUser::getId), ""), null, null);
            // Mesurés en prod : classes C# (pas les tables), sans parent ; finalisation libellée par son type de dossier.
            case ComSalesCommission ignore -> new Trace("Yes.Cfg.SalesCommission", "Commission vendeur", "Commission vendeur",
                    null, null);
            case CfgTrancheFolderEntity f -> new Trace("Yes.Cfg.CfgTrancheFolderEntity", "Définition des finalisations",
                    Objects.toString(Champs.de(f.getStrFolderEntity(), StrEntity::getId), ""), null, null);
            case ComDocument ignore -> new Trace("Yes.Com.Sales.ComDocument", "Financement", "Financement", null, null);
            default -> null;
        };
    }
}
