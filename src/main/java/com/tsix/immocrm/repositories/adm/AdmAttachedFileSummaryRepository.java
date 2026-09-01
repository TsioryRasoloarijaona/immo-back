package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AdmAttachedFileSummaryRepository extends JpaRepository<AdmAttachedFileSummary, UUID> {

    /**
     * Pièces jointes d'un objet porteur quelconque — {@code CategoryId = <porteur>.PKey}.
     *
     * <p>{@code AdmAttachedFile} est une table <b>polymorphe</b> : {@code CategoryId} est une
     * référence parent générique, sans FK déclarée, partagée par tous les types d'objets (voir
     * {@link AdmAttachedFileSummary}). Le rattachement se fait donc sur {@code CategoryId} seul.
     *
     * <p>Ne <b>pas</b> ajouter de filtre sur {@code Category} : la tentative a été faite et les
     * données réelles l'ont invalidée — un dossier porte légitimement des pièces dont le
     * {@code Category} vaut "Yes.Com.Sales.Sale" (chèques, contrats, autorisations...) et pas
     * seulement "Dossier de vente…", et le filtre par préfixe les faisait disparaître
     * silencieusement. Une collision d'UUID entre deux porteurs distincts est astronomiquement
     * improbable.
     *
     * <p>Porteurs actuellement utilisés :
     * <ul>
     *   <li>{@code ComFolder} (Dossier) — {@code Category} legacy "Dossier de vente N° … du …" ;</li>
     *   <li>{@code StkItem} (Article) — ~470 plans réels, {@code Category} "Yes.Stk.Building.Item",
     *       {@code AdmAttachedFileType.Code} en {@code PLAN}/{@code PLAN_ARCHITECTE}/… ;</li>
     *   <li>{@code StlSettlement} (Règlement) — 460 preuves de paiement réelles,
     *       {@code Category} "Yes.Stl.Credit.Settlement" (368) ou "Yes.Stl.Debit.Settlement" (92),
     *       la valeur étant entièrement déterminée par {@code IsCredit} (zéro croisement).</li>
     * </ul>
     */
    @Query("SELECT a FROM AdmAttachedFileSummary a LEFT JOIN FETCH a.type WHERE a.categoryId = :porteurId " +
            "ORDER BY a.userCreateDateTime DESC")
    List<AdmAttachedFileSummary> findByCategoryId(UUID porteurId);

    /** Alias historique de {@link #findByCategoryId} pour un Dossier ({@code ComFolder.PKey}). */
    default List<AdmAttachedFileSummary> findByDossierId(UUID dossierId) {
        return findByCategoryId(dossierId);
    }

    /** Alias historique de {@link #findByCategoryId} pour un Article ({@code StkItem.PKey}). */
    default List<AdmAttachedFileSummary> findByArticleId(UUID articleId) {
        return findByCategoryId(articleId);
    }
}
