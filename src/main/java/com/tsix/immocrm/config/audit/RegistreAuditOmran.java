package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
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
    static final String TRANCHE_ETAGE = "Yes.Cfg.CfgTrancheLevel";
    private static final String LIBELLE_TRANCHE = "Tranche";

    /** Types que le journal rend : rien d'autre ne sort de {@code AdmWebHistorics} (revue §10.1). */
    public static final List<String> TYPES = List.of(PROGRAMME, TRANCHE, TRANCHE_ARTICLE, ARTICLE, SOUS_ARTICLE, TERRAIN,
            TRANCHE_ETAGE);

    private static final List<Class<?>> CLASSES = List.of(CfgProject.class, CfgTranche.class, StkItem.class,
            CfgTrancheStkItem.class, CfgTrancheLevel.class, CfgTrancheCoveredFloor.class,
            CfgTrancheSettlementCategory.class, AdmUserHabilitation.class);

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
            // Libellés d'objet mesurés en prod : PKey de la ligne, Sorting, AdmUserId ; sans parent sauf l'étage (§8.10).
            case CfgTrancheLevel l -> new Trace(TRANCHE_ETAGE, "Etage", l.getId().toString(), TRANCHE,
                    Champs.de(l.getCfgTranche(), CfgTranche::getId));
            case CfgTrancheCoveredFloor p -> new Trace("Yes.Cfg.CfgTrancheCoveredFloor", "Prospection de projet",
                    p.getId().toString(), null, null);
            case CfgTrancheSettlementCategory f -> new Trace("Yes.Cfg.CfgTrancheSettlementCategory", "Frais de vente",
                    Objects.toString(f.getSorting(), ""), null, null);
            case AdmUserHabilitation h -> new Trace("Yes.Adm.AdmUserHabilitation", "Habilitation",
                    Objects.toString(Champs.de(h.getAdmUser(), AdmUser::getId), ""), null, null);
            default -> null;
        });
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
