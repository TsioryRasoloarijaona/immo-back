package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AdmAttachedFileSummaryRepository extends JpaRepository<AdmAttachedFileSummary, UUID> {

    /**
     * Pièces jointes of a Dossier — {@code CategoryId = ComFolder.PKey}. Previously also filtered
     * on a "Dossier de vente" category prefix as a safety net, but real data disproved that
     * assumption: a dossier can legitimately carry attachments whose {@code Category} is e.g.
     * "Yes.Com.Sales.Sale" (chèques, contrats, permissions...), not just "Dossier de vente..."
     * placeholders — the prefix filter silently dropped those. Matching on {@code CategoryId}
     * alone is safe: it is a generic, unconstrained parent reference shared with other object
     * types in this table (see {@link com.tsix.immocrm.entities.adm.AdmAttachedFileSummary}),
     * but a UUID collision between a Dossier's {@code PKey} and an unrelated object's
     * {@code PKey} is astronomically unlikely.
     */
    @Query("SELECT a FROM AdmAttachedFileSummary a LEFT JOIN FETCH a.type WHERE a.categoryId = :dossierId " +
            "ORDER BY a.userCreateDateTime DESC")
    List<AdmAttachedFileSummary> findByDossierId(UUID dossierId);

    /**
     * Pièces jointes of an Article ({@code StkItem}) — {@code CategoryId = StkItem.PKey}, same
     * generic polymorphic-parent convention as {@link #findByDossierId}. Confirmed against real
     * data: the bulk of these are house/floor plans (~470 real files), {@code Category} literally
     * {@code "Yes.Stk.Building.Item"}, {@code AdmAttachedFileType.Code} in {@code PLAN}/
     * {@code PLAN_ARCHITECTE}/{@code PLAN_ETAGE}/{@code PLAN_TYPE} — but this matches ANY
     * attachment on the article, not just plans, exactly like the Dossier equivalent doesn't
     * filter by {@code Category} either.
     */
    @Query("SELECT a FROM AdmAttachedFileSummary a LEFT JOIN FETCH a.type WHERE a.categoryId = :articleId " +
            "ORDER BY a.userCreateDateTime DESC")
    List<AdmAttachedFileSummary> findByArticleId(UUID articleId);
}
