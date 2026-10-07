package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.entities.stk.StkItem;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Filtres d'articles partagés par {@link ArticleService} et les listes de {@code ReferentielService}. */
public final class StkItemSpecifications {

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
        return build(trancheId != null ? List.of(trancheId) : null, parentId, topLevelOnly, statut,
                categorieIds, typeIds, null, false, null, null, null, null, null, null, null, null);
    }

    /**
     * @param entitesActivesOnly jointure interne implicite : un article sans société est écarté, à la
     *                           différence de {@code VisibiliteEntites} (assumé, {@code FEATURE-visibilite-transversale-entites.md} R2)
     */
    public static Specification<StkItem> build(List<UUID> trancheIds, UUID parentId, boolean topLevelOnly, StatutArticle statut,
                                         List<UUID> categorieIds, List<UUID> typeIds, String search, boolean entitesActivesOnly,
                                         UUID entiteId, UUID natureId, UUID vocationId, UUID etageId,
                                         BigDecimal prixDe, BigDecimal prixA, BigDecimal surfaceDe, BigDecimal surfaceA) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!CollectionUtils.isEmpty(trancheIds)) {
                predicates.add(root.get("stkHierarchy").get("id").in(trancheIds));
            }
            if (parentId != null) {
                predicates.add(cb.equal(root.get("parent").get("id"), parentId));
            }
            if (topLevelOnly) {
                predicates.add(cb.isNull(root.get("parent")));
            }
            if (statut != null) {
                predicates.add(switch (statut) {
                    case DISPONIBLE -> cb.isNull(root.get("comFolderId"));
                    case OCCUPE -> cb.isNotNull(root.get("comFolderId"));
                });
            }
            if (!CollectionUtils.isEmpty(categorieIds)) {
                predicates.add(root.get("stkItemCategory").get("id").in(categorieIds));
            }
            if (!CollectionUtils.isEmpty(typeIds)) {
                predicates.add(root.get("stkItemType").get("id").in(typeIds));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (entitesActivesOnly) {
                predicates.add(cb.isTrue(
                        root.get("stkHierarchy").get("cfgProject").get("cfgCompany").get("isActive")));
            }
            if (entiteId != null) {
                predicates.add(cb.equal(root.get("stkHierarchy").get("cfgProject").get("cfgCompany").get("id"), entiteId));
            }
            if (natureId != null) {
                predicates.add(cb.equal(root.get("stkKind").get("id"), natureId));
            }
            if (vocationId != null) {
                predicates.add(cb.equal(root.get("stkVocation").get("id"), vocationId));
            }
            if (etageId != null) {
                predicates.add(cb.equal(root.get("stkFloor").get("id"), etageId));
            }
            if (prixDe != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("comSalePrice"), prixDe));
            }
            if (prixA != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("comSalePrice"), prixA));
            }
            if (surfaceDe != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), surfaceDe));
            }
            if (surfaceA != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), surfaceA));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
