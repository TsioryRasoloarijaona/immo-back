package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Filtres d'articles partagés par {@link ArticleService} et les listes de {@code ReferentielService}. */
public final class StkItemSpecifications {

    private static final String STK_HIERARCHY = "stkHierarchy";
    private static final String VENTE = "comFolderId";
    private static final String LOCATION = "rntFolderId";
    private static final String PARENT = "parent";
    private static final String DESCRIPTION = "description";

    private StkItemSpecifications() {
    }

    /** OMRAN compare sans espaces des deux côtés ({@code REPLACE(x,' ','') LIKE}), ici aussi sans casse. */
    private static String motif(String saisie) {
        return "%" + saisie.replace(" ", "").toLowerCase(Locale.ROOT) + "%";
    }

    private static Expression<String> sansEspaces(CriteriaBuilder cb, Expression<String> champ) {
        return cb.lower(cb.function("replace", String.class, champ, cb.literal(" "), cb.literal("")));
    }

    /**
     * {@code IsUnderItem = 0} en plus de {@code topLevelOnly} : des parkings sont des racines marquées
     * annexes, c'est le drapeau qui compte ({@code FIX-selection-articles-hors-annexes.md}). NULL ⇒
     * pas une annexe. Sélection seulement ({@code /referentiels/articles}).
     */
    public static Specification<StkItem> horsAnnexes() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("isUnderItem")),
                cb.isFalse(root.get("isUnderItem")));
    }

    /** Paramètres null ou vides = pas de restriction. */
    public static Specification<StkItem> build(UUID trancheId, UUID parentId, boolean topLevelOnly, StatutArticle statut,
                                         List<UUID> categorieIds, List<UUID> typeIds) {
        return build(new FiltresArticle(trancheId != null ? List.of(trancheId) : null, parentId, topLevelOnly, statut,
                categorieIds, typeIds, null, false, null, null, null, null, null, null, null, null, false, null, null));
    }

    public static Specification<StkItem> build(FiltresArticle filtres) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            ajouterStructure(predicates, filtres, root, cb);
            ajouterClassement(predicates, filtres, root, cb);
            ajouterCaracteristiques(predicates, filtres, root, cb);
            if (StringUtils.hasText(filtres.search())) {
                predicates.add(recherche(motif(filtres.search()), root, query, cb));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Recherche OMRAN réduite aux textes directs et aux références (FEATURE-patrimoine-sous-articles.md R4-7). */
    private static Predicate recherche(String motif, Root<StkItem> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Join<StkItem, CfgTranche> tranche = root.join(STK_HIERARCHY, JoinType.LEFT);
        Join<CfgTranche, CfgProject> projet = tranche.join("cfgProject", JoinType.LEFT);
        List<Expression<String>> champs = new ArrayList<>(List.of(root.get("code"), root.get(DESCRIPTION),
                root.get("titleNumber"), root.get("address"), root.get("notes"),
                root.join(PARENT, JoinType.LEFT).get("code"), tranche.get("code"), projet.get("code"),
                projet.join("cfgCompany", JoinType.LEFT).get("code")));
        for (String reference : List.of("stkItemCategory", "stkItemType", "stkBuilding", "stkFloor", "stkVocation")) {
            champs.add(root.join(reference, JoinType.LEFT).get(DESCRIPTION));
        }
        List<Predicate> correspondances = new ArrayList<>(champs.stream()
                .map(c -> cb.like(sansEspaces(cb, c), motif)).toList());
        // N° dossier : StkItem.ComFolderId n'est pas une association JPA.
        Subquery<UUID> dossier = query.subquery(UUID.class);
        Root<ComFolder> f = dossier.from(ComFolder.class);
        correspondances.add(cb.exists(dossier.select(f.get("id"))
                .where(cb.equal(f.get("id"), root.get(VENTE)), cb.like(sansEspaces(cb, f.get("code")), motif))));
        return cb.or(correspondances.toArray(new Predicate[0]));
    }

    /** Tranches, parent, racines seules et statut d'occupation. */
    private static void ajouterStructure(List<Predicate> predicates, FiltresArticle f, Root<StkItem> root,
                                         CriteriaBuilder cb) {
        if (!CollectionUtils.isEmpty(f.trancheIds())) {
            predicates.add(root.get(STK_HIERARCHY).get("id").in(f.trancheIds()));
        }
        if (f.parentId() != null) {
            predicates.add(cb.equal(root.get(PARENT).get("id"), f.parentId()));
        }
        if (f.topLevelOnly()) {
            predicates.add(cb.isNull(root.get(PARENT)));
        }
        if (f.sousArticlesSeuls()) {
            predicates.add(cb.and(cb.isTrue(root.get("isUnderItem")), cb.isTrue(root.get("isUnderItemOrigin"))));
        }
        if (f.etat() != null) {
            predicates.add(switch (f.etat()) {
                case LIBRE -> cb.and(cb.isNull(root.get(VENTE)), cb.isNull(root.get(LOCATION)));
                case VENDU -> cb.isNotNull(root.get(VENTE));
                case LOUE -> cb.isNotNull(root.get(LOCATION));
            });
        }
        if (f.statut() != null) {
            predicates.add(switch (f.statut()) {
                case DISPONIBLE -> cb.and(cb.isNull(root.get(VENTE)), cb.isNull(root.get(LOCATION)));
                case OCCUPE -> cb.or(cb.isNotNull(root.get(VENTE)), cb.isNotNull(root.get(LOCATION)));
            });
        }
    }

    /** Catégories, types, code parent et entités. */
    private static void ajouterClassement(List<Predicate> predicates, FiltresArticle f, Root<StkItem> root,
                                          CriteriaBuilder cb) {
        if (!CollectionUtils.isEmpty(f.categorieIds())) {
            predicates.add(root.get("stkItemCategory").get("id").in(f.categorieIds()));
        }
        if (!CollectionUtils.isEmpty(f.typeIds())) {
            predicates.add(root.get("stkItemType").get("id").in(f.typeIds()));
        }
        if (StringUtils.hasText(f.codeParent())) {
            predicates.add(cb.like(sansEspaces(cb, root.get(PARENT).get("code")), motif(f.codeParent())));
        }
        if (f.entitesActivesOnly()) {
            predicates.add(cb.isTrue(
                    root.get(STK_HIERARCHY).get("cfgProject").get("cfgCompany").get("isActive")));
        }
        if (f.entiteId() != null) {
            predicates.add(cb.equal(root.get(STK_HIERARCHY).get("cfgProject").get("cfgCompany").get("id"),
                    f.entiteId()));
        }
    }

    /** Nature, vocation, étage, fourchettes de prix et de surface. */
    private static void ajouterCaracteristiques(List<Predicate> predicates, FiltresArticle f, Root<StkItem> root,
                                                CriteriaBuilder cb) {
        if (f.natureId() != null) {
            predicates.add(cb.equal(root.get("stkKind").get("id"), f.natureId()));
        }
        if (f.vocationId() != null) {
            predicates.add(cb.equal(root.get("stkVocation").get("id"), f.vocationId()));
        }
        if (f.etageId() != null) {
            predicates.add(cb.equal(root.get("stkFloor").get("id"), f.etageId()));
        }
        if (f.prixDe() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("comSalePrice"), f.prixDe()));
        }
        if (f.prixA() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("comSalePrice"), f.prixA()));
        }
        if (f.surfaceDe() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("area"), f.surfaceDe()));
        }
        if (f.surfaceA() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("area"), f.surfaceA()));
        }
    }
}
