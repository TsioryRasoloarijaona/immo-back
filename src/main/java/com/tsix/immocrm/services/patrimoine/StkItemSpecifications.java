package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.entities.stk.StkItem;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Filtres d'articles partagés par {@link ArticleService} et les listes de {@code ReferentielService}. */
public final class StkItemSpecifications {

    private static final String STK_HIERARCHY = "stkHierarchy";

    private StkItemSpecifications() {
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
                categorieIds, typeIds, null, false, null, null, null, null, null, null, null, null));
    }

    public static Specification<StkItem> build(FiltresArticle filtres) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            ajouterStructure(predicates, filtres, root, cb);
            ajouterClassement(predicates, filtres, root, cb);
            ajouterCaracteristiques(predicates, filtres, root, cb);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Tranches, parent, racines seules et statut d'occupation. */
    private static void ajouterStructure(List<Predicate> predicates, FiltresArticle f, Root<StkItem> root,
                                         CriteriaBuilder cb) {
        if (!CollectionUtils.isEmpty(f.trancheIds())) {
            predicates.add(root.get(STK_HIERARCHY).get("id").in(f.trancheIds()));
        }
        if (f.parentId() != null) {
            predicates.add(cb.equal(root.get("parent").get("id"), f.parentId()));
        }
        if (f.topLevelOnly()) {
            predicates.add(cb.isNull(root.get("parent")));
        }
        if (f.statut() != null) {
            predicates.add(switch (f.statut()) {
                case DISPONIBLE -> cb.isNull(root.get("comFolderId"));
                case OCCUPE -> cb.isNotNull(root.get("comFolderId"));
            });
        }
    }

    /** Catégories, types, recherche libre et entités. */
    private static void ajouterClassement(List<Predicate> predicates, FiltresArticle f, Root<StkItem> root,
                                          CriteriaBuilder cb) {
        if (!CollectionUtils.isEmpty(f.categorieIds())) {
            predicates.add(root.get("stkItemCategory").get("id").in(f.categorieIds()));
        }
        if (!CollectionUtils.isEmpty(f.typeIds())) {
            predicates.add(root.get("stkItemType").get("id").in(f.typeIds()));
        }
        if (StringUtils.hasText(f.search())) {
            String pattern = "%" + f.search().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
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
