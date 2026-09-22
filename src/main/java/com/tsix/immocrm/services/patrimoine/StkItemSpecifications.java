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

/**
 * Shared {@link Specification} builder for {@code StkItem} (Article/sous-article) listings —
 * used by both {@link ArticleService} (GET /api/tranches/{id}/articles,
 * GET /api/articles/{id}/sous-articles) and {@code ReferentielService}'s dropdown endpoints
 * (GET /api/referentiels/articles, GET /api/referentiels/sous-articles) so the two stay in sync
 * instead of drifting into two slightly different filter implementations.
 */
public final class StkItemSpecifications {

    private StkItemSpecifications() {
    }

    /**
     * Écarte les <b>annexes</b> de la sélection d'articles — la règle du légataire, relevée dans
     * sa trace d'audit le 22/09/2026 (écran Articles rejoué) : {@code IsUnderItem = 0}, en plus
     * de {@code ParentId IS NULL} (le {@code topLevelOnly} de {@link #build}). Les deux critères
     * sont nécessaires : <b>241 parkings sont des racines</b> (sans parent) mais portent
     * {@code IsUnderItem = 1} — c'est le drapeau, pas la parenté ni la catégorie, qui marque
     * l'annexe. Mesuré sur les 1 068 articles réels : PARKING/TERRASSE/JARDIN tous à 1,
     * APPARTEMENT/COMMERCE/LOT DE TERRAIN tous à 0, zéro NULL — et l'article principal des 358
     * dossiers est toujours un APPARTEMENT (353) ou un COMMERCE (5).
     *
     * <p>Un {@code NULL} (inexistant en base réelle) est traité comme « pas une annexe » :
     * écarter en silence un lot légitime sur une donnée incomplète serait pire que d'afficher
     * une annexe de trop. Composable avec {@link #build}, appliqué à la sélection uniquement
     * ({@code GET /api/referentiels/articles}) : le listing de tranche, le catalogue et le
     * dropdown des sous-articles continuent de tout montrer.
     */
    public static Specification<StkItem> horsAnnexes() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("isUnderItem")),
                cb.isFalse(root.get("isUnderItem")));
    }

    /**
     * @param trancheId    restricts to one tranche ({@code StkItem.stkHierarchy.id}); null = no restriction
     * @param parentId     restricts to the sous-articles of one article ({@code StkItem.parent.id}); null = no restriction
     * @param topLevelOnly when true, excludes sous-articles ({@code StkItem.parent IS NULL})
     * @param statut       DISPONIBLE ({@code ComFolderId IS NULL}) or OCCUPE; null = no restriction
     * @param categorieIds OR-matched on {@code StkItem.stkItemCategory.id}; null/empty = no restriction
     * @param typeIds      OR-matched on {@code StkItem.stkItemType.id}; null/empty = no restriction
     */
    public static Specification<StkItem> build(UUID trancheId, UUID parentId, boolean topLevelOnly, StatutArticle statut,
                                         List<UUID> categorieIds, List<UUID> typeIds) {
        return build(trancheId != null ? List.of(trancheId) : null, parentId, topLevelOnly, statut,
                categorieIds, typeIds, null, false, null, null, null, null, null, null, null, null);
    }

    /**
     * Full form used by {@code GET /api/articles} (catalogue transverse) and, for the
     * legacy-grid-parity filters (instructions/from_front.md #4), also
     * {@code GET /api/tranches/{id}/articles} — adds a repeatable {@code trancheIds} OR-filter
     * (vs. the single-tranche form above, scoped by path variable elsewhere), a free-text
     * {@code search} on {@code code}/{@code description}, the ADMOD/GHRIZA "entité juridique
     * active" restriction ({@code CfgCompany.isActive}) so the transverse catalogue
     * doesn't surface legacy test entities' lots alongside real ones, and the legacy-grid filters
     * confirmed missing (entité/nature/vocation/étage, prix/surface de-à).
     *
     * @param trancheIds           OR-matched on {@code StkItem.stkHierarchy.id}; null/empty = no restriction
     * @param search               case-insensitive substring match on {@code code} OR {@code description}; blank = no restriction
     * @param entitesActivesOnly   when true, restricts to items whose {@code StkItem.stkHierarchy.cfgProject.cfgCompany.isActive} is true
     * @param entiteId             {@code StkItem.stkHierarchy.cfgProject.cfgCompany.id}; null = no restriction
     * @param natureId             {@code StkItem.stkKind.id}; null = no restriction
     * @param vocationId           {@code StkItem.stkVocation.id}; null = no restriction
     * @param etageId              {@code StkItem.stkFloor.id}; null = no restriction
     * @param prixDe/prixA         inclusive range on {@code StkItem.comSalePrice} ("Prix de vente" on the legacy grid); either bound optional
     * @param surfaceDe/surfaceA   inclusive range on {@code StkItem.area}; either bound optional
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
                // Exhaustive switch: a future 3rd StatutArticle value fails to compile here
                // instead of silently falling through to the wrong predicate.
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
