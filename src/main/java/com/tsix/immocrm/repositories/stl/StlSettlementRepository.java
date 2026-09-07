package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlSettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StlSettlementRepository extends JpaRepository<StlSettlement, UUID>, JpaSpecificationExecutor<StlSettlement> {

    /** {@code GET /api/prospects/{id}/reglements} (GAP-06) — every settlement for this tier, across all Dossiers. */
    Page<StlSettlement> findByTierIdOrderByDocDateDesc(UUID tierId, Pageable pageable);

    /**
     * Point de contrôle unique du périmètre sur le chargement d'un encaissement par id — voir
     * {@code PerimetreInterceptor}. Un règlement est dans le périmètre s'il porte au moins une
     * ligne rattachée à un dossier d'un projet du périmètre, <b>ou</b> si aucune de ses lignes ne
     * résout de projet du tout (§6.4 — un règlement non rattaché n'appartient à aucun périmètre et
     * n'est masqué à personne). Même motif de sous-requête {@code EXISTS} que le filtre
     * {@code dossierId} de {@code EncaissementService}.
     */
    @Query("SELECT COUNT(s) > 0 FROM StlSettlement s LEFT JOIN s.cfgCompany soc "
            + "WHERE s.id = :id AND (soc.isActive = true OR soc.id IS NULL) AND ("
            + "EXISTS (SELECT 1 FROM StlSettlementLine l WHERE l.stlSettlement = s "
            + "        AND l.comDocument.comFolder.stkItem.stkHierarchy.cfgProject.id IN :projetIds) "
            + "OR NOT EXISTS (SELECT 1 FROM StlSettlementLine l2 WHERE l2.stlSettlement = s "
            + "        AND l2.comDocument.comFolder.stkItem.stkHierarchy.cfgProject.id IS NOT NULL))")
    boolean existsDansPerimetre(@Param("id") UUID id, @Param("projetIds") List<UUID> projetIds);

    /**
     * Variante de {@link #existsDansPerimetre} pour un appelant non restreint par un périmètre :
     * seule la visibilité par entité juridique s'applique (R4).
     *
     * <p>Le règlement porte sa société en FK directe ({@code StlSettlement.CfgCompanyId}), mesurée
     * concordante avec celle déduite du dossier sur 644 règlements de vente sur 644 — inutile de
     * repasser par les lignes ici.
     */
    @Query("SELECT COUNT(s) > 0 FROM StlSettlement s LEFT JOIN s.cfgCompany soc "
            + "WHERE s.id = :id AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsVisible(@Param("id") UUID id);
}
