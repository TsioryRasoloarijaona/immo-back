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
 * <b>La</b> définition de « appartient à la tranche », pour un dossier et pour une prospection.
 *
 * <p>Source unique partagée par les filtres {@code trancheId} de {@code GET /api/dossiers},
 * {@code GET /api/prospections} / {@code GET /api/pre-prospections}, et par le filtre par tranche
 * du tableau de bord et de {@code GET /api/clients}
 * ({@code instructions/FEATURE-dashboard-filtre-tranche.md}, R2). Un second prédicat écrit à part
 * ferait diverger les compteurs du tableau de bord des totaux des listes filtrées.
 */
public final class AppartenanceTranche {

    private AppartenanceTranche() {
    }

    /**
     * Un dossier appartient à l'une des tranches si l'un de ses articles liés
     * ({@code StkItem.comFolderId}) <b>ou</b> son article principal ({@code ComFolder.stkItem}) y
     * appartient. Les deux branches sont nécessaires : le lien inverse ne couvre que 328 des 358
     * dossiers réels, l'article principal les 358. Un dossier peut porter plusieurs articles
     * (lot principal + annexes) éventuellement répartis sur plusieurs tranches, d'où
     * l'{@code EXISTS} plutôt qu'une simple jointure — et un tel dossier compte dans chacune.
     * Voir {@code instructions/filtres-avances.md} point 2.
     *
     * @param dossier le dossier, racine d'une requête ou d'une sous-requête
     * @param query   la requête (ou sous-requête) qui porte {@code dossier}, pour y ouvrir l'EXISTS
     */
    public static Predicate dossier(From<?, ComFolder> dossier, CommonAbstractCriteria query,
                                    CriteriaBuilder cb, Collection<UUID> trancheIds) {
        Subquery<UUID> subquery = query.subquery(UUID.class);
        Root<StkItem> article = subquery.from(StkItem.class);
        subquery.select(article.get("id"))
                .where(cb.and(
                        cb.equal(article.get("comFolderId"), dossier.get("id")),
                        article.get("stkHierarchy").get("id").in(trancheIds)));
        // L'EXISTS seul ne suffit pas : il passe par le lien inverse StkItem.ComFolderId, qui
        // n'est renseigné que pour 328 des 358 dossiers réels. Les 30 autres ont pourtant bien une
        // tranche via leur article principal (ComFolder.StkItemId, lui renseigné à 358/358) — sans
        // cette seconde branche ils étaient invisibles au filtre, silencieusement.
        //
        // Jointures explicitement LEFT : une navigation implicite produirait des INNER JOIN qui,
        // portant sur toute la requête et non sur la seule branche du OR, écarteraient un dossier
        // sans article principal même quand l'EXISTS le retient.
        Join<ComFolder, StkItem> principal = dossier.join("stkItem", JoinType.LEFT);
        Join<StkItem, CfgTranche> trancheDuPrincipal = principal.join("stkHierarchy", JoinType.LEFT);
        return cb.or(cb.exists(subquery), trancheDuPrincipal.get("id").in(trancheIds));
    }

    /** {@link #dossier(From, CommonAbstractCriteria, CriteriaBuilder, Collection)} en {@code Specification}. */
    public static Specification<ComFolder> dossier(UUID trancheId) {
        return (root, query, cb) -> dossier(root, query, cb, List.of(trancheId));
    }

    /**
     * Une prospection (ou pré-prospection) appartient à la tranche qu'elle vise :
     * {@code ComProspection.StkHierarchyId}, clé étrangère directe — pas d'EXISTS nécessaire.
     */
    public static Predicate prospection(From<?, ComProspection> prospection, Collection<UUID> trancheIds) {
        return prospection.get("stkHierarchy").get("id").in(trancheIds);
    }
}
