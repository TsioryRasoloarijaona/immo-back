package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgCompanyRepository extends JpaRepository<CfgCompany, UUID> {

    /** {@code = true} et non {@code != false} : un drapeau jamais renseigné reste invisible. */
    @Query("SELECT c FROM CfgCompany c WHERE c.isActive = true ORDER BY c.description ASC")
    List<CfgCompany> findActivesOrderByDescriptionAsc();

    /** Collation French_CI_AS : l'égalité SQL ignore déjà la casse et les espaces de fin. */
    Optional<CfgCompany> findFirstByCode(String code);

    boolean existsByCfgGroupShareholder_Id(UUID groupeId);
}
