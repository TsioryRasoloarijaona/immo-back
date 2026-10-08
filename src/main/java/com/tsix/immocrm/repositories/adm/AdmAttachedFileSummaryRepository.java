package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AdmAttachedFileSummaryRepository extends JpaRepository<AdmAttachedFileSummary, UUID> {

    /**
     * Pièces d'un porteur quelconque (dossier, article, règlement) par {@code CategoryId} seul. Pas de
     * filtre sur {@code Category} : il faisait disparaître les pièces de dossier classées "Yes.Com.Sales.Sale".
     */
    @Query("SELECT a FROM AdmAttachedFileSummary a LEFT JOIN FETCH a.type WHERE a.categoryId = :porteurId " +
            "ORDER BY a.userCreateDateTime DESC")
    List<AdmAttachedFileSummary> findByCategoryId(UUID porteurId);

    default List<AdmAttachedFileSummary> findByDossierId(UUID dossierId) {
        return findByCategoryId(dossierId);
    }

    default List<AdmAttachedFileSummary> findByArticleId(UUID articleId) {
        return findByCategoryId(articleId);
    }
}
