package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.domain.chantier.LigneMontant;
import com.tsix.immocrm.entities.prj.PrjMarketLineEstimation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PrjMarketLineEstimationRepository extends JpaRepository<PrjMarketLineEstimation, UUID> {

    @Query("SELECT new com.tsix.immocrm.domain.chantier.LigneMontant(l.prjMarket.id, l.quantity, l.unitPriceHT, "
            + "l.vatRatio, l.unitPriceTTC) FROM PrjMarketLineEstimation l WHERE l.prjMarket.id IN :lotIds "
            + "AND (l.isStock IS NULL OR l.isStock = false)")
    List<LigneMontant> findLignesMontant(@Param("lotIds") Collection<UUID> lotIds);
}
