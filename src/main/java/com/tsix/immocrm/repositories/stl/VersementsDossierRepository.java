package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.UUID;

/** Versements du schéma de financement d'un dossier (avance, auto-financement, crédit), pour le workflow. */
public interface VersementsDossierRepository extends JpaRepository<StlSettlementLine, UUID> {

    /**
     * Date du dernier règlement d'une échéance avance / auto-financement / crédit au crédit
     * ({@code Sale.cs:GetDateToAffectForTransition}) ; nul sans règlement.
     */
    @Query("SELECT MAX(st.docDate) FROM StlSettlementLine l JOIN l.stlSettlement st JOIN l.comDocument d "
            + "JOIN d.comSettlementCategory c WHERE d.comFolder.id = :dossier AND c.isCredit = true "
            + "AND (c.isSaleAdvance = true OR c.isSaleAutoFinancment = true OR c.isSaleCredit = true) AND "
            + StatutReglement.JPQL_COMPTE_DANS_LES_TOTAUX)
    LocalDateTime dernierVersement(UUID dossier);
}
