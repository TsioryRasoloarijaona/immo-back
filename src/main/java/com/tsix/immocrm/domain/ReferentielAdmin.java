package com.tsix.immocrm.domain;

import com.tsix.immocrm.domain.ChampReferentiel.Type;
import com.tsix.immocrm.entities.cfg.CfgTrancheStanding;
import com.tsix.immocrm.entities.cfg.CfgTrancheStatus;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.lnd.LndVocation;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkContenance;
import com.tsix.immocrm.entities.stk.StkCostCategory;
import com.tsix.immocrm.entities.stk.SpecificFunction;
import com.tsix.immocrm.entities.stk.StkExpenseType;
import com.tsix.immocrm.entities.stk.StkFSBaseUnit;
import com.tsix.immocrm.entities.stk.StkFacade;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemExonaration;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkOrientation;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkPresentationTypeContenance;
import com.tsix.immocrm.entities.stk.StkTrancheHeading;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stk.StkVue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.domain.ChampReferentiel.CODE;
import static com.tsix.immocrm.domain.ChampReferentiel.COURT;
import static com.tsix.immocrm.domain.ChampReferentiel.DESCRIPTION;
import static com.tsix.immocrm.domain.ChampReferentiel.LONG;
import static com.tsix.immocrm.domain.ChampReferentiel.NOTES;
import static com.tsix.immocrm.domain.ChampReferentiel.SORTING;
import static com.tsix.immocrm.domain.ChampReferentiel.de;
import static com.tsix.immocrm.domain.ChampReferentiel.entier;
import static com.tsix.immocrm.domain.ChampReferentiel.reference;
import static com.tsix.immocrm.domain.ChampReferentiel.texte;

/**
 * Référentiels écrits par {@code /api/referentiels-admin/{slug}} (FEATURE-patrimoine-referentiels.md §2, §5.2, §8) :
 * une seule implémentation pilotée par ce descripteur ; les règles propres à une table sont les méthodes surchargées.
 */
public enum ReferentielAdmin {
    CATEGORIES("categories", "Catégories", Famille.ARTICLES, StkItemCategory.class,
            new Audit("Yes.Stk.Config.ItemCategory", "Catégorie d'article"), Colonnes.PAR_NATURE, Colonnes.CLE_NATURE),
    TYPES("types", "Types", Famille.ARTICLES, StkItemType.class,
            new Audit("Yes.Stk.Config.ItemType", "Type d'article"), Colonnes.PAR_NATURE, Colonnes.CLE_NATURE),
    IMMEUBLES("immeubles", "Immeubles", Famille.ARTICLES, StkBuilding.class,
            new Audit("Yes.Stk.Config.StkBuilding", "Immeuble"), Colonnes.TRI_CODE_LIBELLE, Colonnes.CLE_CODE),
    ETAGES("etages", "Étages", Famille.ARTICLES, StkFloor.class,
            new Audit("Yes.Stk.Config.StkFloor", "Etage"), Colonnes.TRI_CODE_LIBELLE, Colonnes.CLE_CODE) {
        @Override
        public List<Bloquant> bloquants() {
            return List.of(new Bloquant("SELECT COUNT(p) FROM StkHierarchyPath p WHERE p.stkFloorId = :id",
                    "StkHierarchyPath (tracés de plan)"));
        }
    },
    ORIENTATIONS("orientations", "Orientations", Famille.ARTICLES, StkOrientation.class,
            new Audit("Yes.Stk.Config.StkOrientation", "Orientation"), Colonnes.TRI_CODE_LIBELLE, Colonnes.CLE_CODE),
    VOCATIONS("vocations", "Vocations", Famille.ARTICLES, StkVocation.class,
            new Audit("Yes.Stk.Config.Vocation", "Vocation"), Colonnes.CODE_LIBELLE, Colonnes.CLE_CODE) {
        @Override
        public Set<UUID> idsProteges() {
            return Set.of(UUID.fromString("7E729AD8-A030-4D20-91CF-9D8E9D89C318"),
                    UUID.fromString("4814C424-F23B-48BB-8E54-020FAA27AF24"));
        }

        @Override
        public Set<String> codesProteges() {
            return Set.of("VENTE", "PROJET");
        }
    },
    NATURES("natures", "Natures", Famille.ARTICLES, StkKind.class,
            new Audit("Yes.Stk.Config.Kind", "Nature"), Colonnes.CODE_LIBELLE, Colonnes.CLE_CODE) {
        @Override
        public Set<UUID> idsProteges() {
            return Set.of(UUID.fromString("BEDD4235-FBE5-41FA-B5C1-B7EE83C90DE0"),
                    UUID.fromString("343710CB-8806-484E-BB2C-9073569C72A4"),
                    UUID.fromString("4BDDEF6B-6732-449C-97C5-1FB318D6C444"),
                    UUID.fromString("4BDDEF6B-1111-2222-3333-1FB318D6C444"));
        }

        @Override
        public Set<String> codesProteges() {
            return Set.of("BAT", "TER", "PROS TER", "LOT DE TERRAIN");
        }

        @Override
        public List<Bloquant> bloquants() {
            return List.of(new Bloquant("SELECT COUNT(c) FROM StkItemCategory c WHERE c.stkKind.id = :id",
                            "StkItemCategory (catégories)"),
                    new Bloquant("SELECT COUNT(t) FROM StkItemType t WHERE t.stkKind.id = :id", "StkItemType (types)"));
        }
    },
    FACADES("facades", "Façades", Famille.ARTICLES, StkFacade.class,
            new Audit("Yes.Stk.Config.Facade", "Façade"), Colonnes.CODE_LIBELLE, Colonnes.CLE_CODE),
    VUES("vues", "Vues", Famille.ARTICLES, StkVue.class,
            new Audit("Yes.Stk.Config.Vue", "Vue"), Colonnes.CODE_LIBELLE, Colonnes.CLE_CODE) {
        @Override
        public List<Bloquant> bloquants() {
            return List.of(new Bloquant("SELECT COUNT(v) FROM StkItemVue v WHERE v.stkVue.id = :id",
                    "StkItemVue (vues d'article)"));
        }
    },
    CATEGORIES_CHARGE("categories-charge", "Catégories de charge", Famille.ARTICLES, StkCostCategory.class,
            new Audit("Yes.Stk.Config.CostCategory", "Catégorie de charge"), Colonnes.CODE_LIBELLE, Colonnes.CLE_CODE),
    CONTENANCES(Colonnes.CONTENANCES_PRESENTATION, "Contenances", Famille.ARTICLES, StkContenance.class,
            new Audit("Yes.Stk.Config.Contenance", "Contenance"), Colonnes.CONTENANCE, Colonnes.CLE_CODE) {
        @Override
        public List<Bloquant> bloquants() {
            return List.of(new Bloquant("SELECT COUNT(c) FROM StkItemContenance c WHERE c.stkContenance.id = :id",
                    "StkItemContenance (contenances d'article)"));
        }
    },
    TYPES_PRESENTATION("types-presentation", "Types de présentation", Famille.ARTICLES, StkPresentationType.class,
            new Audit("Yes.Stk.Config.PresentationType", "Type de présentation"), Colonnes.PRESENTATION,
            List.of("stkHierarchy", "code")) {
        @Override
        public Optional<SousLignes> sousLignes() {
            return Optional.of(new SousLignes(Colonnes.CONTENANCES_PRESENTATION, StkPresentationTypeContenance.class,
                    "stkPresentationType",
                    "SELECT c FROM StkPresentationTypeContenance c WHERE c.stkPresentationType.id = :id ORDER BY c.id ASC",
                    Colonnes.LIGNE_CONTENANCE));
        }
    },
    TYPES_FRAIS("types-frais", "Types de frais", Famille.ARTICLES, StkExpenseType.class,
            new Audit("Yes.Stk.Config.ExpenseType", "Type des Frais"), Colonnes.TYPE_FRAIS, Colonnes.CLE_CODE),
    STANDINGS("standings", "Standings", Famille.PROSPECTIONS_TERRAINS, CfgTrancheStanding.class,
            new Audit("Yes.Cfg.TrancheStanding", "Standing"), Colonnes.CODE_LIBELLE, Colonnes.CLE_CODE),
    VOCATIONS_TERRAIN("vocations-terrain", "Vocations terrain", Famille.PROSPECTIONS_TERRAINS, LndVocation.class,
            new Audit("Yes.Lnd.Config.Vocation", "Vocation"), Colonnes.CODE_LIBELLE, Colonnes.CLE_CODE),
    NIVEAUX_TERRAIN("niveaux-terrain", "Niveaux terrain", Famille.PROSPECTIONS_TERRAINS, LndLevel.class,
            new Audit("Yes.Lnd.Config.Level", "Niveau"), Colonnes.CODE_LIBELLE, Colonnes.CLE_CODE),
    QUOTES_PARTS_EXONEREES("quotes-parts-exonerees", "Quote part exonérée", Famille.AUTRES, StkItemExonaration.class,
            new Audit("Yes.Stk.Config.StkItemExonaration", "Quote part exonérée"), Colonnes.EXONERATION,
            List.of(Colonnes.CFG_TRANCHE, "docYear")),
    RUBRIQUES_TRANCHE("rubriques-tranche", "Rubriques de tranche", Famille.AUTRES, StkTrancheHeading.class,
            new Audit("Yes.Stk.Config.TrancheHeading", "Rubrique de tranche"), Colonnes.RUBRIQUE, Colonnes.CLE_CODE),
    /** Écran limité aux dossiers de vente ; unicité (tranche, entité, statut) : le contrôle lit {@code findFirst()}. */
    SEUILS_VERSEMENT("seuils-versement", "Seuils de versement", Famille.AUTRES, CfgTrancheStatus.class,
            new Audit("Yes.Cfg.CfgTrancheStatus", "Seuil de versement"), Colonnes.SEUIL,
            List.of(Colonnes.CFG_TRANCHE, "strFolderEntityId", "strStatusId")) {
        @Override
        public Map<String, Object> portee() {
            return Map.of("strFolderEntityId", StatutDossier.CONFIG_ENTITY_ID);
        }
    },
    /** Études de faisabilité (FEATURE-patrimoine-faisabilite.md §3) : table sans colonne d'audit. */
    UNITES_BASE("unites-de-base", "Unités de base", Famille.AUTRES, StkFSBaseUnit.class,
            new Audit("Yes.Stk.BalanceSheet.FSBaseUnit", "Unité de base"), Colonnes.UNITE_BASE, Colonnes.CLE_CODE) {
        @Override
        public List<Bloquant> bloquants() {
            return List.of(new Bloquant("SELECT COUNT(l) FROM StkFSBaseUnitStkFeasibilityStudy l "
                    + "WHERE l.stkFSBaseUnit.id = :id", "StkFSBaseUnitStkFeasibilityStudy (unités d'étude)"));
        }
    },
    FONCTIONS_SPECIFIQUES("fonctions-specifiques", "Fonctions spécifiques", Famille.AUTRES, SpecificFunction.class,
            new Audit("Yes.Stk.Config.SpecificFunction", "Fonction spécifique"), Colonnes.FONCTION, Colonnes.CLE_CODE) {
        @Override
        public List<Bloquant> bloquants() {
            return List.of(new Bloquant("SELECT COUNT(u) FROM StkFSBaseUnit u WHERE u.specificFunction.id = :id",
                    "StkFSBaseUnit (unités de base)"));
        }
    };

    public enum Famille {
        ARTICLES, PROSPECTIONS_TERRAINS, AUTRES
    }

    /** {@code ObjectType} et {@code ObjectTypeLabel} écrits dans {@code AdmWebHistorics}. */
    public record Audit(String type, String libelle) {
    }

    /** Table enfant en CASCADE qu'on refuse de vider en silence (§8.1) : {@code jpql} compte les lignes de {@code :id}. */
    public record Bloquant(String jpql, String table) {
    }

    /** Lignes enfants saisies dans le corps du parent ({@code nom}) et remplacées en entier à chaque écriture. */
    public record SousLignes(String nom, Class<?> classe, String proprieteParent, String jpql,
                             List<ChampReferentiel> champs) {
    }

    private final String slug;
    private final String libelle;
    private final Famille famille;
    private final Class<?> classe;
    private final Audit audit;
    private final List<ChampReferentiel> champs;
    private final List<String> cle;

    ReferentielAdmin(String slug, String libelle, Famille famille, Class<?> classe, Audit audit,
                     List<ChampReferentiel> champs, List<String> cle) {
        this.slug = slug;
        this.libelle = libelle;
        this.famille = famille;
        this.classe = classe;
        this.audit = audit;
        this.champs = champs;
        this.cle = cle;
    }

    public String slug() {
        return slug;
    }

    public String libelle() {
        return libelle;
    }

    public Famille famille() {
        return famille;
    }

    public Class<?> classe() {
        return classe;
    }

    public Audit audit() {
        return audit;
    }

    public List<ChampReferentiel> champs() {
        return champs;
    }

    /** Propriétés dont la combinaison est unique dans la table (U, ou U* composite) ; une valeur nulle compte. */
    public List<String> cle() {
        return cle;
    }

    /** Lignes jamais supprimées ni recodées : PKey constantes d'OMRAN… */
    public Set<UUID> idsProteges() {
        return Set.of();
    }

    /** … et codes que le CRM résout en dur ({@code "VENTE"}, {@code "BAT"}…). */
    public Set<String> codesProteges() {
        return Set.of();
    }

    public List<Bloquant> bloquants() {
        return List.of();
    }

    public Optional<SousLignes> sousLignes() {
        return Optional.empty();
    }

    /** Valeurs imposées à la création et seules lignes visibles (propriété → valeur). */
    public Map<String, Object> portee() {
        return Map.of();
    }

    /** Propriété de la tranche portée par la ligne (visibilité R4), sinon vide. */
    public Optional<String> proprieteTranche() {
        return champs.stream().filter(c -> c.reference() == ReferenceAdmin.TRANCHE).map(ChampReferentiel::propriete)
                .findFirst();
    }

    public static Optional<ReferentielAdmin> parSlug(String slug) {
        return Arrays.stream(values()).filter(r -> r.slug.equals(slug)).findFirst();
    }

    public static Optional<ReferentielAdmin> parClasse(Class<?> classe) {
        return Arrays.stream(values()).filter(r -> r.classe == classe).findFirst();
    }

    /** Listes de champs partagées : une classe à part, les constantes d'une énumération ne pouvant s'y référer. */
    private static final class Colonnes {
        static final String CFG_TRANCHE = "cfgTranche";
        static final List<String> CLE_CODE = List.of("code");
        static final List<String> CLE_NATURE = List.of("stkKind", "code");
        static final List<ChampReferentiel> CODE_LIBELLE = List.of(CODE, DESCRIPTION);
        static final List<ChampReferentiel> TRI_CODE_LIBELLE = List.of(SORTING.requis(), CODE, DESCRIPTION);
        static final ChampReferentiel TRANCHE = reference("trancheId", "Tranche", CFG_TRANCHE, ReferenceAdmin.TRANCHE);
        static final ChampReferentiel CATEGORIE_COMPTABLE = reference("categorieComptableId", "Catégorie comptable",
                "stlCategory", ReferenceAdmin.CATEGORIE_COMPTABLE).requis();
        static final List<ChampReferentiel> PAR_NATURE = List.of(
                reference("natureId", "Nature", "stkKind", ReferenceAdmin.NATURE).requis(), CODE, DESCRIPTION,
                CATEGORIE_COMPTABLE, texte("descriptionEdition", "Description d'édition", "reportDescription", LONG),
                de("coefficientVariationStock", "Coef. variation de stock", Type.DECIMAL, "stockVariationCoef"),
                de("pourSousArticles", "Pour les sous-articles", Type.BOOLEEN, "forUnderItem"),
                de("venteSurface", "Vente à la surface", Type.BOOLEEN, "withAreaSale"), NOTES);
        static final List<ChampReferentiel> CONTENANCE = Stream.concat(Stream.of(SORTING, CODE, DESCRIPTION),
                options("option%dAvecQuantite", "isOption%dWithQuantity", "Option %d avec quantité")).toList();
        static final String CONTENANCES_PRESENTATION = "contenances";
        static final List<ChampReferentiel> PRESENTATION = List.of(
                reference("trancheId", "Tranche", "stkHierarchy", ReferenceAdmin.TRANCHE).requis(), CODE, DESCRIPTION,
                de("couleur", "Couleur", Type.COULEUR, "colorName"),
                de(CONTENANCES_PRESENTATION, "Contenances", Type.LIGNES, null));
        static final List<ChampReferentiel> LIGNE_CONTENANCE = Stream.concat(Stream.of(
                        reference("contenanceId", "Contenance", "stkContenance", ReferenceAdmin.CONTENANCE).requis(),
                        entier("nombre", "Nombre", "itemsNumber")),
                Stream.concat(options("avecOption%d", "isWithOption%d", "Avec option %d"),
                        Stream.of(1, 2, 3).map(n -> entier("quantiteOption" + n, "Quantité option " + n,
                                "quantityOption" + n)))).toList();
        static final List<ChampReferentiel> TYPE_FRAIS = List.of(CODE, DESCRIPTION, CATEGORIE_COMPTABLE,
                reference("typeMarcheId", "Type de marché", "prjMarketType", ReferenceAdmin.TYPE_MARCHE).requis());
        static final List<ChampReferentiel> EXONERATION = List.of(TRANCHE.requis(),
                entier("annee", "Année", "docYear").requis(), de("coutUnitaire", "Coût m²", Type.DECIMAL, "unitCost"),
                NOTES);
        static final List<ChampReferentiel> RUBRIQUE = List.of(CODE, DESCRIPTION, SORTING, NOTES);
        static final List<ChampReferentiel> SEUIL = List.of(TRANCHE,
                reference("statutId", "Statut", "strStatusId", ReferenceAdmin.STATUT_VENTE).requis(),
                reference("typeReglementId", "Type de règlement", "comFolderStatusPaymentType",
                        ReferenceAdmin.TYPE_REGLEMENT).requis(),
                de("operateur", "Opérateur", Type.OPERATEUR, "thresholdOperator").requis(),
                de("ratio", "Seuil", Type.FRACTION, "thresholdRatio").requis());
        static final List<ChampReferentiel> UNITE_BASE = List.of(SORTING.requis(), CODE, DESCRIPTION,
                reference("fonctionSpecifiqueId", "Fonction spécifique", "specificFunction",
                        ReferenceAdmin.FONCTION_SPECIFIQUE),
                reference("categorieArticleId", "Catégorie d'article", "stkItemCategory", ReferenceAdmin.CATEGORIE_ARTICLE),
                de("estSurface", "Surface", Type.BOOLEEN, "isArea"));
        static final List<ChampReferentiel> FONCTION = List.of(SORTING.requis(), CODE, DESCRIPTION,
                de("estTotal", "Total", Type.BOOLEEN, "isTotal"),
                de("surfaceVendable", "Surface vendable", Type.BOOLEEN, "saleAbleArea"),
                de("plancherCouvert", "Plancher couvert", Type.BOOLEEN, "coveredArea"),
                de("surfaceNette", "Surface nette", Type.BOOLEEN, "areaNet"),
                de("surfaceBrute", "Surface brute", Type.BOOLEEN, "areaBrut"),
                de("empriseAuSol", "Emprise au sol", Type.BOOLEEN, "areaCos"));

        /** Trois options (texte) et leur case, numérotées 1 à 3 dans les trois motifs. */
        private static Stream<ChampReferentiel> options(String nomCase, String proprieteCase, String libelleCase) {
            return Stream.of(1, 2, 3).flatMap(n -> Stream.of(
                    texte("option" + n, "Option " + n, "option" + n, COURT),
                    de(nomCase.formatted(n), libelleCase.formatted(n), Type.BOOLEEN, proprieteCase.formatted(n))));
        }

        private Colonnes() {
        }
    }
}
