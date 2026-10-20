package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComSaleWithdrawalCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ComSaleWithdrawalCancellationRepository
        extends JpaRepository<ComSaleWithdrawalCancellation, UUID>, JpaSpecificationExecutor<ComSaleWithdrawalCancellation> {

    /** R1 : un désistement non légalisé bloque le suivant ({@code Verif}). */
    boolean existsByComFolderIdAndLegalizationDateIsNull(UUID dossierId);

    Optional<ComSaleWithdrawalCancellation> findFirstByComFolderIdOrderByClaimDateDescUserCreateDateTimeDesc(UUID dossierId);
}
