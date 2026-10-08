package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CfgTierRepository extends JpaRepository<CfgTier, UUID>, JpaSpecificationExecutor<CfgTier> {

    /**
     * Les quatre colonnes téléphone, saisies sans format homogène (avec ou sans +212, espaces) : les deux
     * côtés gardent les 9 derniers chiffres, comme {@link com.tsix.immocrm.utils.PhoneNumberUtils#normalize(String)}.
     */
    @Query("SELECT t FROM CfgTier t WHERE "
            + "right(replace(replace(replace(replace(replace(replace(coalesce(t.phone, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone "
            + "OR right(replace(replace(replace(replace(replace(replace(coalesce(t.phone1, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone "
            + "OR right(replace(replace(replace(replace(replace(replace(coalesce(t.phone2, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone "
            + "OR right(replace(replace(replace(replace(replace(replace(coalesce(t.mobile, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone")
    List<CfgTier> findByNormalizedPhone(@Param("normalizedPhone") String normalizedPhone);

    /**
     * Trois colonnes : l'ordre des mots de {@code Description} n'est pas garanti. Native pour {@code COLLATE} :
     * la base est en {@code French_CI_AS}, « Hejer » ne trouverait pas « Héjér ». {@code limite} borne le
     * coût par fiche ; demander {@code limite + 1} signale la troncature (FEATURE-hotline-recherche-nom.md §2).
     */
    @Query(value = "SELECT TOP (:limite) t.* FROM CfgTier t WHERE "
            + "t.Description COLLATE French_CI_AI LIKE :motif "
            + "OR t.FirstName COLLATE French_CI_AI LIKE :motif "
            + "OR t.LastName COLLATE French_CI_AI LIKE :motif "
            + "ORDER BY t.Description",
            nativeQuery = true)
    List<CfgTier> rechercherParNom(@Param("motif") String motif, @Param("limite") int limite);

    /** Banques : {@code CfgTierType.Code = "BAN"}. */
    List<CfgTier> findByCfgTierType_CodeOrderByDescriptionAsc(String code);

    /**
     * Suit le pointeur de doublon {@code ComProspection.TempField10}. Une {@code List}, jamais un
     * {@code Optional} : {@code Code} n'est pas unique, un {@code Optional} lèverait sur un doublon de
     * code (FEATURE-marqueurs-tri-pre-prospection.md §3).
     */
    List<CfgTier> findByCode(String code);

    /**
     * Contrôle d'OMRAN rendu disjonctif (nom ou téléphone ou email ; OMRAN exige nom et email, jamais
     * vrai sans email) — FEATURE-doublons-prospect-projet.md §2. Paramètres normalisés, {@code ""} si
     * absents : un critère vide ne matche jamais.
     */
    @Query("SELECT t FROM CfgTier t JOIN t.cfgTierType ty "
            + "WHERE lower(ty.code) IN ('pro', 'prornt', 'cli', 'clirnt') AND ("
            + "(:cle <> '' AND ("
            + "lower(replace(concat(coalesce(t.firstName, ''), coalesce(t.lastName, '')), ' ', '')) = :cle "
            + "OR lower(replace(concat(coalesce(t.lastName, ''), coalesce(t.firstName, '')), ' ', '')) = :cle "
            + "OR lower(replace(coalesce(t.description, ''), ' ', '')) = :cle "
            + "OR lower(replace(coalesce(t.socialReason, ''), ' ', '')) = :cle)) "
            + "OR (:tel1 <> '' AND (right(replace(replace(replace(replace(replace(replace(coalesce(t.phone, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :tel1 OR right(replace(replace(replace(replace(replace(replace(coalesce(t.phone1, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :tel1 OR right(replace(replace(replace(replace(replace(replace(coalesce(t.phone2, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :tel1 OR right(replace(replace(replace(replace(replace(replace(coalesce(t.mobile, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :tel1)) "
            + "OR (:tel2 <> '' AND (right(replace(replace(replace(replace(replace(replace(coalesce(t.phone, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :tel2 OR right(replace(replace(replace(replace(replace(replace(coalesce(t.phone1, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :tel2 OR right(replace(replace(replace(replace(replace(replace(coalesce(t.phone2, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :tel2 OR right(replace(replace(replace(replace(replace(replace(coalesce(t.mobile, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :tel2)) "
            + "OR (:email <> '' AND lower(trim(coalesce(t.eMail, ''))) = :email)) "
            + "ORDER BY t.userCreateDateTime DESC")
    List<CfgTier> trouverDoublons(@Param("cle") String cle, @Param("tel1") String tel1,
                                  @Param("tel2") String tel2, @Param("email") String email,
                                  Pageable pageable);
}
