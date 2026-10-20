package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.Contributions;
import com.tsix.immocrm.domain.ReferentielAdmin;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.stk.StkFSBaseUnit;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemExonaration;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.utils.Champs;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Entités tracées dans {@code AdmWebHistorics} et leurs libellés OMRAN, mesurés en production
 * (FEATURE-patrimoine-audit-actions.md §1, §2.1). Chaque domaine apporte les siennes par une classe {@link TracesAudit}.
 */
public final class RegistreAuditOmran {

    static final String PROGRAMME = "Yes.Cfg.CfgProject";
    static final String TRANCHE = "Yes.Cfg.CfgTranche";
    static final String TRANCHE_ARTICLE = "Yes.Cfg.CfgTrancheStkItem";
    static final String ARTICLE = "Yes.Stk.Building.Item";
    static final String SOUS_ARTICLE = "Yes.Stk.Building.UnderItem";
    static final String TERRAIN = "Yes.Stk.LandProjectItem";
    static final String TRANCHE_ETAGE = "Yes.Cfg.CfgTrancheLevel";
    static final String ENTITE = "Yes.Cfg.CfgCompany";
    static final String REPARTITION = "Yes.Cfg.CfgCompanyShareholder";
    static final String ACTIONNAIRE = "Yes.Cfg.CfgShareholder";
    static final String GROUPE = "Yes.Cfg.CfgGroupShareholder";
    static final String LIGNE_GROUPE = "Yes.Cfg.CfgGroupShareholderLine";
    // Codes OMRAN (StkItemVue.cs, ItemContenance.cs) : aucune ligne en prod (FEATURE-patrimoine-article-fiche.md §11.8).
    static final String VUE_ARTICLE = "Yes.Stk.Building.StkItemVue";
    static final String CONTENANCE_ARTICLE = "Yes.Stk.Building.ItemContenance";
    static final String LIBELLE_TRANCHE = "Tranche";

    private static final List<TracesAudit> DOMAINES = Contributions.trouver(TracesAudit.class);

    /** Types que le journal rend : rien d'autre ne sort de {@code AdmWebHistorics} (revue §10.1). */
    public static final List<String> TYPES = DOMAINES.stream().flatMap(d -> d.types().stream()).distinct().toList();

    private static final Set<Class<?>> CLASSES = DOMAINES.stream().flatMap(d -> d.classes().stream())
            .collect(Collectors.toUnmodifiableSet());

    private RegistreAuditOmran() {
    }

    /** {@code type} → {@code ObjectType} ; {@code nomCourt} = dernier segment, écrit par OMRAN dans une suppression. */
    public record Trace(String type, String libelleType, String libelleObjet, String typeParent, UUID parentId) {

        String nomCourt() {
            return type.substring(type.lastIndexOf('.') + 1);
        }
    }

    static boolean trace(Class<?> classe) {
        return CLASSES.contains(classe) || ReferentielAdmin.parClasse(classe).isPresent();
    }

    /** Une mise à jour qui vaut suppression : la pièce jointe supprimée logiquement. */
    static boolean suppressionLogique(Object entite) {
        return entite instanceof AdmAttachedFile f && f.isSuppressionLogique();
    }

    static Optional<Trace> tracer(Object entite) {
        return DOMAINES.stream().map(d -> d.tracer(entite)).filter(Objects::nonNull).findFirst()
                .or(() -> ReferentielAdmin.parClasse(entite.getClass()).map(r -> referentiel(r, entite)));
    }

    /** Référentiel du lot P6 : libellé d'objet = Description (mesuré en prod), à défaut année ou PKey. */
    private static Trace referentiel(ReferentielAdmin r, Object entite) {
        String libelle = switch (entite) {
            case CodeLibelleOmran c -> c.getDescription();
            case StkItemExonaration e -> Objects.toString(e.getDocYear(), "");
            case StkFSBaseUnit u -> u.getDescription();
            case LigneOmran l -> l.getId().toString();
            default -> "";
        };
        return new Trace(r.audit().type(), r.audit().libelle(), libelle, null, null);
    }

    /** Ligne enfant d'un article : parent = l'article, typé selon sa nature (article, sous-article, terrain). */
    static Trace ligneArticle(String type, String libelleType, String libelle, StkItem article) {
        return new Trace(type, libelleType, libelle, article != null ? article(article).type() : null,
                Champs.de(article, StkItem::getId));
    }

    /** ObjectLabel : article = Code ; sous-article, terrain = Description (mesuré en prod). */
    static Trace article(StkItem item) {
        if (Boolean.TRUE.equals(item.getIsUnderItem())) {
            return new Trace(SOUS_ARTICLE, "Sous article", item.getDescription(), null, null);
        }
        if ("TER".equalsIgnoreCase(Champs.de(item.getStkKind(), StkKind::getCode))) {
            return new Trace(TERRAIN, "Terrain", item.getDescription(), null, null);
        }
        return new Trace(ARTICLE, "Article", item.getCode(), null, null);
    }
}
