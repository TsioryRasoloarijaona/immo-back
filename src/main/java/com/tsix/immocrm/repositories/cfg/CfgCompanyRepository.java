package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CfgCompanyRepository extends JpaRepository<CfgCompany, UUID> {

    /** Sociétés où le compte a une habilitation ({@code CfgCompanyView.GetList}) ; {@code tous} = rôle exempté. */
    String HABILITEE = "(:tous = true OR c.id IN (SELECT h.cfgCompany.id FROM AdmUserHabilitation h "
            + "WHERE h.admUser.id = :compteId AND h.cfgCompany IS NOT NULL))";

    /** {@code = true} et non {@code != false} : un drapeau jamais renseigné reste invisible. */
    @Query("SELECT c FROM CfgCompany c WHERE c.isActive = true ORDER BY c.description ASC")
    List<CfgCompany> findActivesOrderByDescriptionAsc();

    /**
     * Écran « Entités juridiques », actives ou non : {@code actif} null = toutes ; {@code motif} = {@code %texte%}
     * sans espaces, comparé aux colonnes privées de leurs espaces, comme OMRAN.
     */
    @Query("SELECT c FROM CfgCompany c WHERE " + HABILITEE
            + " AND (:actif IS NULL OR c.isActive = :actif) AND (:motif IS NULL "
            + "OR REPLACE(c.code, ' ', '') LIKE :motif OR REPLACE(c.description, ' ', '') LIKE :motif "
            + "OR REPLACE(c.fiscalNumber, ' ', '') LIKE :motif OR REPLACE(c.commercialRegister, ' ', '') LIKE :motif) "
            + "ORDER BY c.code ASC")
    List<CfgCompany> rechercherHabilitees(@Param("tous") boolean tous, @Param("compteId") UUID compteId,
                                          @Param("actif") Boolean actif, @Param("motif") String motif);

    @Query("SELECT c FROM CfgCompany c WHERE c.id = :id AND " + HABILITEE)
    Optional<CfgCompany> trouverHabilitee(@Param("id") UUID id, @Param("tous") boolean tous,
                                          @Param("compteId") UUID compteId);

    /** Unicité du code sur toute la table (collation French_CI_AS : « = » ignore la casse). */
    Optional<CfgCompany> findFirstByCodeAndIdNot(String code, UUID id);

    List<CfgCompany> findByCfgGroupShareholderId(UUID groupeId);
}
