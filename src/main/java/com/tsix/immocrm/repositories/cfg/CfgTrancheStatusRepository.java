package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTrancheStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CfgTrancheStatusRepository extends JpaRepository<CfgTrancheStatus, UUID> {

    /** Seuil d'un statut pour une tranche ({@code CfgTrancheStatus.GetByStatus(statusId, trancheId)}). */
    @Query("SELECT ts FROM CfgTrancheStatus ts LEFT JOIN FETCH ts.comFolderStatusPaymentType "
            + "WHERE ts.strStatusId = :statut AND ts.cfgTranche.id = :tranche ORDER BY ts.id ASC")
    List<CfgTrancheStatus> deLaTranche(UUID statut, UUID tranche);

    /** Seuil global d'un statut ({@code GetByStatus(statusId, null)}). */
    @Query("SELECT ts FROM CfgTrancheStatus ts LEFT JOIN FETCH ts.comFolderStatusPaymentType "
            + "WHERE ts.strStatusId = :statut AND ts.cfgTranche IS NULL ORDER BY ts.id ASC")
    List<CfgTrancheStatus> global(UUID statut);
}
