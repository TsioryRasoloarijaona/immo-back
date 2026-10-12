package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.domain.chantier.LigneMontant;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Bordereau définitif = lignes hors stock ({@code IsStock} 0 ou NULL). */
public interface PrjMarketLineRepository extends JpaRepository<PrjMarketLine, UUID> {

    String HORS_STOCK = "(l.isStock IS NULL OR l.isStock = false)";

    @Query("SELECT new com.tsix.immocrm.domain.chantier.LigneMontant(l.prjMarket.id, l.quantity, l.unitPriceHT, "
            + "l.vatRatio, l.unitPriceTTC) FROM PrjMarketLine l WHERE l.prjMarket.id IN :lotIds AND " + HORS_STOCK)
    List<LigneMontant> findLignesMontant(@Param("lotIds") Collection<UUID> lotIds);

    @Query("SELECT l FROM PrjMarketLine l WHERE l.prjMarket.id = :lotId AND " + HORS_STOCK
            + " ORDER BY l.sorting ASC, l.code ASC")
    List<PrjMarketLine> findBordereau(@Param("lotId") UUID lotId);
}
