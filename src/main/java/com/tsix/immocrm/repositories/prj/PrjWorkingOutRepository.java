package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrjWorkingOutRepository extends JpaRepository<PrjWorkingOut, UUID>,
        JpaSpecificationExecutor<PrjWorkingOut> {

    String AVEC_REFERENTIELS = "SELECT w FROM PrjWorkingOut w JOIN FETCH w.prjMarket m "
            + "LEFT JOIN FETCH m.prjProject LEFT JOIN FETCH w.cfgTier "
            + "LEFT JOIN FETCH w.buyDocument b LEFT JOIN FETCH b.buyDocumentStatus ";

    @Query(AVEC_REFERENTIELS + "WHERE w.id IN :ids")
    List<PrjWorkingOut> findAllAvecReferentiels(@Param("ids") Collection<UUID> ids);

    @Query(AVEC_REFERENTIELS + "WHERE w.id = :id")
    Optional<PrjWorkingOut> findAvecReferentiels(@Param("id") UUID id);

    @Query(AVEC_REFERENTIELS + "WHERE m.id = :lotId ORDER BY w.sorting ASC, w.docDate ASC")
    List<PrjWorkingOut> findByLot(@Param("lotId") UUID lotId);

    /** Tuple : [lotId, nombre, ΣAmountHT, ΣAmountTTC, ΣextraAmount, ΣWarrantyAmount, ΣAmountToPay]. */
    @Query("SELECT w.prjMarket.id, COUNT(w), SUM(w.amountHT), SUM(w.amountTTC), SUM(w.extraAmount), "
            + "SUM(w.warrantyAmount), SUM(w.amountToPay) FROM PrjWorkingOut w "
            + "WHERE w.prjMarket.id IN :lotIds GROUP BY w.prjMarket.id")
    List<Object[]> sommesParLot(@Param("lotIds") Collection<UUID> lotIds);

    /** Tuple : [lotId, factureId, BuyDocument.AmountToPay] — une ligne par couple distinct. */
    @Query("SELECT DISTINCT w.prjMarket.id, b.id, b.amountToPay FROM PrjWorkingOut w JOIN w.buyDocument b "
            + "WHERE w.prjMarket.id IN :lotIds")
    List<Object[]> facturesParLot(@Param("lotIds") Collection<UUID> lotIds);

    /** Cumul TTC jusqu'à ce numéro inclus : seul calcul autorisé sur un décompte. */
    @Query("SELECT SUM(w.amountTTC) FROM PrjWorkingOut w WHERE w.prjMarket.id = :lotId AND w.sorting <= :numero")
    BigDecimal cumulTTCJusquA(@Param("lotId") UUID lotId, @Param("numero") Integer numero);
}
