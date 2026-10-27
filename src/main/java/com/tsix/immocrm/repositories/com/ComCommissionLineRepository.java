package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComCommissionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ComCommissionLineRepository extends JpaRepository<ComCommissionLine, UUID> {

    List<ComCommissionLine> findByCfgTrancheIdIn(Collection<UUID> trancheIds);

    List<ComCommissionLine> findByComFolderIdIn(Collection<UUID> dossierIds);

    List<ComCommissionLine> findByBuyDocumentIdIn(Collection<UUID> documentIds);

    @Query("SELECT DISTINCT l.buyDocumentId FROM ComCommissionLine l WHERE l.cfgTrancheId = :trancheId "
            + "AND l.buyDocumentId IS NOT NULL")
    List<UUID> documentsDeLaTranche(UUID trancheId);
}
