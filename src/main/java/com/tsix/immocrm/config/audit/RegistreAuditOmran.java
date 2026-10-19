package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.entities.cfg.LigneActionnaireOmran;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.utils.Champs;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Entités tracées dans {@code AdmWebHistorics} et leurs libellés OMRAN, mesurés en production
 * (FEATURE-patrimoine-audit-actions.md §1, §2.1). Un lot qui écrit une nouvelle table l'ajoute ici.
 */
public final class RegistreAuditOmran {

    static final String PROGRAMME = "Yes.Cfg.CfgProject";
    static final String TRANCHE = "Yes.Cfg.CfgTranche";
    static final String TRANCHE_ARTICLE = "Yes.Cfg.CfgTrancheStkItem";
    static final String ARTICLE = "Yes.Stk.Building.Item";
    static final String SOUS_ARTICLE = "Yes.Stk.Building.UnderItem";
    static final String TERRAIN = "Yes.Stk.LandProjectItem";
    static final String ENTITE = "Yes.Cfg.CfgCompany";
    static final String REPARTITION = "Yes.Cfg.CfgCompanyShareholder";
    static final String ACTIONNAIRE = "Yes.Cfg.CfgShareholder";
    static final String GROUPE = "Yes.Cfg.CfgGroupShareholder";
    static final String LIGNE_GROUPE = "Yes.Cfg.CfgGroupShareholderLine";
    private static final String LIBELLE_TRANCHE = "Tranche";

    /** Types que le journal rend : rien d'autre ne sort de {@code AdmWebHistorics} (revue §10.1). */
    public static final List<String> TYPES = List.of(PROGRAMME, TRANCHE, TRANCHE_ARTICLE, ARTICLE, SOUS_ARTICLE, TERRAIN,
            ENTITE, REPARTITION, ACTIONNAIRE, GROUPE, LIGNE_GROUPE);

    private static final List<Class<?>> CLASSES = List.of(CfgProject.class, CfgTranche.class, StkItem.class,
            CfgTrancheStkItem.class, CfgCompany.class, CfgCompanyShareholder.class, CfgShareholder.class,
            CfgGroupShareholder.class, CfgGroupShareholderLine.class);

    private RegistreAuditOmran() {
    }

    /** {@code type} → {@code ObjectType} ; {@code nomCourt} = dernier segment, écrit par OMRAN dans une suppression. */
    public record Trace(String type, String libelleType, String libelleObjet, String typeParent, UUID parentId) {

        String nomCourt() {
            return type.substring(type.lastIndexOf('.') + 1);
        }
    }

    static boolean trace(Class<?> classe) {
        return CLASSES.contains(classe);
    }

    /** ObjectLabel : programme et article = Code ; tranche, sous-article, terrain = Description (mesuré en prod). */
    static Optional<Trace> tracer(Object entite) {
        return Optional.ofNullable(switch (entite) {
            case CfgProject p -> new Trace(PROGRAMME, "Programme immobilier", p.getCode(), null, null);
            case CfgTranche t -> new Trace(TRANCHE, LIBELLE_TRANCHE, t.getDescription(), null, null);
            case StkItem i -> article(i);
            case CfgTrancheStkItem l -> new Trace(TRANCHE_ARTICLE, LIBELLE_TRANCHE, Objects.toString(l.getSorting(), ""),
                    TRANCHE, Champs.de(l.getCfgTranche(), CfgTranche::getId));
            case CfgCompany c -> new Trace(ENTITE, "Entité juridique", c.getCode(), null, null);
            case CfgCompanyShareholder r -> new Trace(REPARTITION, "Répartition", codeActionnaire(r), ENTITE,
                    Champs.de(r.getCfgCompany(), CfgCompany::getId));
            case CfgShareholder a -> new Trace(ACTIONNAIRE, "Actionnaire", a.getCode(), null, null);
            case CfgGroupShareholder g -> new Trace(GROUPE, "Groupe d'actionnaires", g.getCode(), null, null);
            case CfgGroupShareholderLine l -> new Trace(LIGNE_GROUPE, "Ligne de groupe", codeActionnaire(l), GROUPE,
                    Champs.de(l.getCfgGroupShareholder(), CfgGroupShareholder::getId));
            default -> null;
        });
    }

    private static String codeActionnaire(LigneActionnaireOmran ligne) {
        return Champs.de(ligne.getCfgShareholder(), CfgShareholder::getCode);
    }

    private static Trace article(StkItem item) {
        if (Boolean.TRUE.equals(item.getIsUnderItem())) {
            return new Trace(SOUS_ARTICLE, "Sous article", item.getDescription(), null, null);
        }
        if ("TER".equalsIgnoreCase(Champs.de(item.getStkKind(), StkKind::getCode))) {
            return new Trace(TERRAIN, "Terrain", item.getDescription(), null, null);
        }
        return new Trace(ARTICLE, "Article", item.getCode(), null, null);
    }
}
