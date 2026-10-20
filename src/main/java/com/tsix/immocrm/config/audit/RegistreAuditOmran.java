package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.domain.ReferentielAdmin;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItemExonaration;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkPresentationTypeContenance;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.entities.cfg.LigneActionnaireOmran;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.stk.StkContenance;
import com.tsix.immocrm.entities.stk.StkHierarchyPlan;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemContenance;
import com.tsix.immocrm.entities.stk.StkItemVue;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVue;
import com.tsix.immocrm.entities.cfg.CfgTrancheFolderEntity;
import com.tsix.immocrm.entities.cfg.ComSalesCommission;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComSaleWithdrawalCancellation;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.stk.StkFSBaseUnit;
import com.tsix.immocrm.entities.stk.StkFSBaseUnitStkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyCfgTranche;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyCfgTrancheStkBaseUnit;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatusHistory;
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
    static final String ENTITE = "Yes.Cfg.CfgCompany";
    static final String REPARTITION = "Yes.Cfg.CfgCompanyShareholder";
    static final String ACTIONNAIRE = "Yes.Cfg.CfgShareholder";
    static final String GROUPE = "Yes.Cfg.CfgGroupShareholder";
    static final String LIGNE_GROUPE = "Yes.Cfg.CfgGroupShareholderLine";
    // Codes OMRAN (StkItemVue.cs, ItemContenance.cs) : aucune ligne en prod (FEATURE-patrimoine-article-fiche.md §11.8).
    static final String VUE_ARTICLE = "Yes.Stk.Building.StkItemVue";
    static final String CONTENANCE_ARTICLE = "Yes.Stk.Building.ItemContenance";
    private static final String LIBELLE_TRANCHE = "Tranche";
    private static final String PRESENTATION = ReferentielAdmin.TYPES_PRESENTATION.audit().type();

    /** Types que le journal rend : rien d'autre ne sort de {@code AdmWebHistorics} (revue §10.1). */
    public static final List<String> TYPES = List.of(PROGRAMME, TRANCHE, TRANCHE_ARTICLE, ARTICLE, SOUS_ARTICLE, TERRAIN,
            TRANCHE_ETAGE, ENTITE, REPARTITION, ACTIONNAIRE, GROUPE, LIGNE_GROUPE, VUE_ARTICLE, CONTENANCE_ARTICLE);

    private static final List<Class<?>> CLASSES = List.of(CfgProject.class, CfgTranche.class, StkItem.class,
            CfgTrancheStkItem.class, CfgTrancheLevel.class, CfgTrancheCoveredFloor.class,
            CfgTrancheSettlementCategory.class, AdmUserHabilitation.class, CfgCompany.class,
            CfgCompanyShareholder.class, CfgShareholder.class, CfgGroupShareholder.class, CfgGroupShareholderLine.class,
            StkItemVue.class, StkItemContenance.class,
            ComSalesCommission.class, CfgTrancheFolderEntity.class, ComDocument.class,
            AdmAttachedFile.class, StkHierarchyPlan.class,
            StkFeasibilityStudy.class, StkFSBaseUnitStkFeasibilityStudy.class, StkFeasibilityStudyCfgTranche.class,
            StkFeasibilityStudyCfgTrancheStkBaseUnit.class, StkFeasibilityStudyStatusHistory.class,
            ComSaleWithdrawalCancellation.class);

    private RegistreAuditOmran() {
    }

    /** {@code type} → {@code ObjectType} ; {@code nomCourt} = dernier segment, écrit par OMRAN dans une suppression. */
    public record Trace(String type, String libelleType, String libelleObjet, String typeParent, UUID parentId) {

        String nomCourt() {
            return type.substring(type.lastIndexOf('.') + 1);
        }
    }

    static boolean trace(Class<?> classe) {
        return CLASSES.contains(classe) || classe == StkPresentationTypeContenance.class
                || ReferentielAdmin.parClasse(classe).isPresent();
    }

    /** Une mise à jour qui vaut suppression : la pièce jointe supprimée logiquement. */
    static boolean suppressionLogique(Object entite) {
        return entite instanceof AdmAttachedFile f && f.isSuppressionLogique();
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
            // Libellés d'objet mesurés en prod : PKey de la ligne, Sorting, AdmUserId ; sans parent sauf l'étage (§8.10).
            case CfgTrancheLevel l -> new Trace(TRANCHE_ETAGE, "Etage", l.getId().toString(), TRANCHE,
                    Champs.de(l.getCfgTranche(), CfgTranche::getId));
            case CfgTrancheCoveredFloor p -> new Trace("Yes.Cfg.CfgTrancheCoveredFloor", "Prospection de projet",
                    p.getId().toString(), null, null);
            case CfgTrancheSettlementCategory f -> new Trace("Yes.Cfg.CfgTrancheSettlementCategory", "Frais de vente",
                    Objects.toString(f.getSorting(), ""), null, null);
            case AdmUserHabilitation h -> new Trace("Yes.Adm.AdmUserHabilitation", "Habilitation",
                    Objects.toString(Champs.de(h.getAdmUser(), AdmUser::getId), ""), null, null);
            case StkItemVue v -> ligneArticle(VUE_ARTICLE, "Vues", Champs.de(v.getStkVue(), StkVue::getDescription),
                    v.getStkItem());
            case StkItemContenance c -> ligneArticle(CONTENANCE_ARTICLE, "Contenances",
                    Champs.de(c.getStkContenance(), StkContenance::getDescription), c.getStkItem());
            // Mesurés en prod : classes C# (pas les tables), sans parent ; finalisation libellée par son type de dossier.
            case ComSalesCommission ignore -> new Trace("Yes.Cfg.SalesCommission", "Commission vendeur", "Commission vendeur",
                    null, null);
            case CfgTrancheFolderEntity f -> new Trace("Yes.Cfg.CfgTrancheFolderEntity", "Définition des finalisations",
                    Objects.toString(Champs.de(f.getStrFolderEntity(), StrEntity::getId), ""), null, null);
            case ComDocument ignore -> new Trace("Yes.Com.Sales.ComDocument", "Financement", "Financement", null, null);
            case StkPresentationTypeContenance c -> new Trace("Yes.Stk.Config.PresentationTypeContenance", "Contenance",
                    Champs.de(c.getStkContenance(), StkContenance::getDescription), PRESENTATION,
                    Champs.de(c.getStkPresentationType(), StkPresentationType::getId));
            // Tous porteurs, sans parent : absente de TYPES, l'onglet Audit d'un porteur ne la lit pas (P2b §8.3).
            case AdmAttachedFile f -> new Trace("Yes.Adm.Attachment.AttachmentWithData", "Pièce jointe",
                    f.getLibelleAudit(), null, null);
            case StkHierarchyPlan p -> new Trace("Yes.Cfg.StkHierarchyPlan", "Plan", p.getCode(), null, null);
            // Études de faisabilité (FEATURE-patrimoine-faisabilite.md §2.6) : sans parent ; les deux derniers types
            // ne sont pas mesurés en prod, ObjectType = classe OMRAN.
            case StkFeasibilityStudy e -> new Trace("Yes.Stk.StkFeasibilityStudy", "Etude de faisabilité",
                    e.getDescription(), null, null);
            case StkFSBaseUnitStkFeasibilityStudy l -> new Trace("Yes.Stk.BalanceSheet.FSBaseUnitStkFeasibilityStudy",
                    "Prévision", Champs.de(l.getStkFSBaseUnit(), StkFSBaseUnit::getCode), null, null);
            case StkFeasibilityStudyCfgTranche ignore -> new Trace("Yes.Stk.BalanceSheet.FeasibilityStudyCfgTranche",
                    LIBELLE_TRANCHE, LIBELLE_TRANCHE, null, null);
            case StkFeasibilityStudyCfgTrancheStkBaseUnit v -> new Trace(
                    "Yes.Stk.BalanceSheet.FeasibilityStudyCfgTrancheStkBaseUnit", "Unité de la tranche",
                    v.getId().toString(), null, null);
            case StkFeasibilityStudyStatusHistory h -> new Trace("Yes.Stk.Config.FeasibilityStudyStatusHistory",
                    "Historique des états", h.getId().toString(), null, null);
            case ComSaleWithdrawalCancellation w -> new Trace("Yes.Com.Sales.SaleWithdrawalCancellation", "Désistement",
                    w.getCode(), null, null);
            default -> ReferentielAdmin.parClasse(entite.getClass()).map(r -> referentiel(r, entite)).orElse(null);
        });
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
    private static Trace ligneArticle(String type, String libelleType, String libelle, StkItem article) {
        return new Trace(type, libelleType, libelle, article != null ? article(article).type() : null,
                Champs.de(article, StkItem::getId));
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
