package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.stl.StlCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PrjMarketRepository extends JpaRepository<PrjMarket, UUID>, JpaSpecificationExecutor<PrjMarket> {

    /** Toutes les associations lues par les DTO, en une requête (la page vient d'une Specification sans fetch). */
    @Query("SELECT m FROM PrjMarket m LEFT JOIN FETCH m.prjMarketType LEFT JOIN FETCH m.cfgSupplier "
            + "LEFT JOIN FETCH m.stlCategory LEFT JOIN FETCH m.strStatus LEFT JOIN FETCH m.strEntity "
            + "LEFT JOIN FETCH m.prjProject p LEFT JOIN FETCH p.cfgCompany WHERE m.id IN :ids")
    List<PrjMarket> findAllAvecReferentiels(@Param("ids") Collection<UUID> ids);

    /** Lots des projets donnés, sans fetch : seuls leurs colonnes et l'id du projet sont lus (totaux projet). */
    @Query("SELECT m FROM PrjMarket m WHERE m.prjProject.id IN :projetIds")
    List<PrjMarket> findByProjetIds(@Param("projetIds") Collection<UUID> projetIds);

    @Query("SELECT m FROM PrjMarket m WHERE m.prjProject IS NOT NULL")
    List<PrjMarket> findAvecProjet();

    @Query("SELECT DISTINCT c FROM PrjMarket m JOIN m.stlCategory c")
    List<StlCategory> findCategoriesReferencees();

    @Query("SELECT DISTINCT c FROM PrjMarket m JOIN m.stlCategory c WHERE m.prjProject.id IN :projetIds")
    List<StlCategory> findCategoriesReferencees(@Param("projetIds") Collection<UUID> projetIds);
}
