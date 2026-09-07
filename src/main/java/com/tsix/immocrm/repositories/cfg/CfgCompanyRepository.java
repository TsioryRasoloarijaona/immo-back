package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CfgCompanyRepository extends JpaRepository<CfgCompany, UUID> {

    /**
     * {@code GET /api/referentiels/entites} — entités en exploitation, triées par libellé.
     *
     * <p>Le filtre est {@code isActive = true} et non {@code != false} : une entité dont le
     * drapeau n'a jamais été renseigné reste invisible. Voir {@code CfgCompany.isActive}.
     */
    @Query("SELECT c FROM CfgCompany c WHERE c.isActive = true ORDER BY c.description ASC")
    List<CfgCompany> findActivesOrderByDescriptionAsc();
}
