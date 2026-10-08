package com.tsix.immocrm.services.tranche;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * <b>La</b> définition de « appartient à la tranche », partagée par les listes, le tableau de bord et
 * les clients : un second prédicat ferait diverger compteurs et totaux ({@code FEATURE-dashboard-filtre-tranche.md} R2).
 */
public final class AppartenanceTranche {

    private static final String STK_HIERARCHY = "stkHierarchy";

    private AppartenanceTranche() {
    }

    /**
     * Un article lié ({@code StkItem.comFolderId}) <b>ou</b> l'article principal dans la tranche. Un
     * dossier aux articles répartis sur plusieurs tranches compte dans chacune, d'où l'{@code EXISTS}.
     *
     * @param query la requête (ou sous-requête) qui porte {@code dossier}, pour y ouvrir l'EXISTS
     */
    public static Predicate dossier(From<?, ComFolder> dossier, CommonAbstractCriteria query,
                                    CriteriaBuilder cb, Collection<UUID> trancheIds) {
        Subquery<UUID> subquery = query.subquery(UUID.class);
        Root<StkItem> article = subquery.from(StkItem.class);
        subquery.select(article.get("id"))
                .where(cb.and(
                        cb.equal(article.get("comFolderId"), dossier.get("id")),
                        article.get(STK_HIERARCHY).get("id").in(trancheIds)));
        // L'EXISTS seul ne suffit pas : le lien inverse StkItem.ComFolderId manque sur des dossiers
        // dont l'article principal a bien une tranche. LEFT explicite : une navigation implicite
        // ferait un INNER JOIN sur toute la requête et écarterait un dossier sans article principal.
        Join<ComFolder, StkItem> principal = dossier.join("stkItem", JoinType.LEFT);
        Join<StkItem, CfgTranche> trancheDuPrincipal = principal.join(STK_HIERARCHY, JoinType.LEFT);
        return cb.or(cb.exists(subquery), trancheDuPrincipal.get("id").in(trancheIds));
    }

    /** {@link #dossier(From, CommonAbstractCriteria, CriteriaBuilder, Collection)} en {@code Specification}. */
    public static Specification<ComFolder> dossier(UUID trancheId) {
        return (root, query, cb) -> dossier(root, query, cb, List.of(trancheId));
    }

    /** FK directe {@code ComProspection.StkHierarchyId}, pré-prospections comprises. */
    public static Predicate prospection(From<?, ComProspection> prospection, Collection<UUID> trancheIds) {
        return prospection.get(STK_HIERARCHY).get("id").in(trancheIds);
    }
}
