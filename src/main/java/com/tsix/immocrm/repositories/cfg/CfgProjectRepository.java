package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgProjectRepository extends JpaRepository<CfgProject, UUID> {

    /** Entité en exploitation ; chemin implicite = jointure interne, un projet sans société est exclu. */
    @Query("SELECT p FROM CfgProject p WHERE p.cfgCompany.isActive = true")
    List<CfgProject> findDesEntitesActives();

    /** {@code motif} = {@code %texte%} en minuscules, ou null pour tout. */
    @Query("SELECT p FROM CfgProject p WHERE p.cfgCompany.isActive = true AND (:motif IS NULL "
            + "OR lower(p.code) LIKE :motif OR lower(p.description) LIKE :motif OR lower(p.city) LIKE :motif "
            + "OR lower(p.cfgCompany.code) LIKE :motif OR lower(p.cfgCompany.description) LIKE :motif)")
    Page<CfgProject> rechercher(@Param("motif") String motif, Pageable pageable);

    /** Programme lu ou écrit par id : 404 quand l'entité est inactive (R4), administrateur compris ; sans entité = visible. */
    @Query("SELECT p FROM CfgProject p LEFT JOIN p.cfgCompany soc WHERE p.id = :id AND " + VisibiliteEntites.CLAUSE_JPQL)
    Optional<CfgProject> findVisible(@Param("id") UUID id);

    /** Collation French_CI_AS : l'égalité SQL ignore déjà la casse et les espaces de fin. */
    Optional<CfgProject> findFirstByCode(String code);
}
