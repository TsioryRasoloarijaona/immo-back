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

    Page<StlSettlement> findByTierIdOrderByDocDateDesc(UUID tierId, Pageable pageable);

    /**
     * Contrôle unique du périmètre par id : une ligne sur un projet du périmètre, ou aucune ligne qui
     * résout un projet (§6.4). Chemins implicites = jointures internes : une ligne sans projet ne compte
     * dans aucun des deux {@code EXISTS}, c'est voulu.
     */
    @Query("SELECT COUNT(s) > 0 FROM StlSettlement s LEFT JOIN s.cfgCompany soc "
            + "WHERE s.id = :id AND (soc.isActive = true OR soc.id IS NULL) AND ("
            + "EXISTS (SELECT 1 FROM StlSettlementLine l WHERE l.stlSettlement = s "
            + "        AND l.comDocument.comFolder.stkItem.stkHierarchy.cfgProject.id IN :projetIds) "
            + "OR NOT EXISTS (SELECT 1 FROM StlSettlementLine l2 WHERE l2.stlSettlement = s "
            + "        AND l2.comDocument.comFolder.stkItem.stkHierarchy.cfgProject.id IS NOT NULL))")
    boolean existsDansPerimetre(@Param("id") UUID id, @Param("projetIds") List<UUID> projetIds);

    /**
     * Appelant sans périmètre : visibilité par entité seule (R4), par la FK directe {@code CfgCompanyId},
     * concordante avec la société du dossier (FEATURE-visibilite-transversale-entites.md).
     */
    @Query("SELECT COUNT(s) > 0 FROM StlSettlement s LEFT JOIN s.cfgCompany soc "
            + "WHERE s.id = :id AND (soc.isActive = true OR soc.id IS NULL)")
    boolean existsVisible(@Param("id") UUID id);
}
