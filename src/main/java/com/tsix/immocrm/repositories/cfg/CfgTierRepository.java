package com.tsix.immocrm.repositories.cfg;

import com.tsix.immocrm.entities.cfg.CfgTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CfgTierRepository extends JpaRepository<CfgTier, UUID>, JpaSpecificationExecutor<CfgTier> {

    /**
     * {@code GET /api/hotline/recherche} — every {@code CfgTier} with a phone/mobile column
     * matching the given number, format-agnostic. Legacy phone data is stored inconsistently
     * (with/without "+212", with/without spaces — confirmed empirically against real
     * {@code yesRems} data), so both sides of the comparison are normalized the same way as
     * {@link com.tsix.immocrm.utils.PhoneNumberUtils#normalize(String)}: strip everything but
     * digits, keep only the last 9 (a Moroccan local number without country code). Checks all
     * four phone columns ({@code Phone}, {@code Phone1}, {@code Phone2}, {@code Mobile}).
     */
    @Query("SELECT t FROM CfgTier t WHERE "
            + "right(replace(replace(replace(replace(replace(replace(coalesce(t.phone, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone "
            + "OR right(replace(replace(replace(replace(replace(replace(coalesce(t.phone1, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone "
            + "OR right(replace(replace(replace(replace(replace(replace(coalesce(t.phone2, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone "
            + "OR right(replace(replace(replace(replace(replace(replace(coalesce(t.mobile, ''), ' ', ''), '-', ''), '.', ''), '(', ''), ')', ''), '+', ''), 9) = :normalizedPhone")
    List<CfgTier> findByNormalizedPhone(@Param("normalizedPhone") String normalizedPhone);

    /**
     * Recherche hotline par nom — {@code Description} (« Ines ALAMI », renseignée sur 100 % des
     * 14 514 lignes réelles), {@code FirstName} et {@code LastName} (97 %). Les trois, parce que
     * l'ordre des mots dans {@code Description} n'est pas garanti : « ALAMI Ines » doit trouver
     * autant que « Ines ALAMI ».
     *
     * <p><b>Requête native pour une seule raison : {@code COLLATE}, que JPQL ne connaît pas.</b>
     * La base est en {@code French_CI_AS} — insensible à la casse mais <b>sensible aux
     * accents</b>. Sans {@code COLLATE French_CI_AI}, « Hejer » ne trouverait pas « Héjér », qui
     * existe réellement en base. Le défaut serait invisible en test avec des noms ASCII et
     * systématique en production.
     *
     * <p>{@code limite} borne le coût : chaque fiche construite déclenche ensuite cinq requêtes,
     * et « MOHAMED » remonte 555 lignes. Demander {@code limite + 1} permet à l'appelant de
     * savoir qu'il a tronqué. Voir {@code instructions/FEATURE-hotline-recherche-nom.md} §2.
     */
    @Query(value = "SELECT TOP (:limite) t.* FROM CfgTier t WHERE "
            + "t.Description COLLATE French_CI_AI LIKE :motif "
            + "OR t.FirstName COLLATE French_CI_AI LIKE :motif "
            + "OR t.LastName COLLATE French_CI_AI LIKE :motif "
            + "ORDER BY t.Description",
            nativeQuery = true)
    List<CfgTier> rechercherParNom(@Param("motif") String motif, @Param("limite") int limite);

    /**
     * {@code GET /api/referentiels/banques} — {@code CfgTier} filtré sur {@code CfgTierType.Code}
     * ("BAN", confirmé sur données réelles : 21 tiers) pour peupler {@code CreerReglementRequest.banqueId}.
     */
    List<CfgTier> findByCfgTierType_CodeOrderByDescriptionAsc(String code);

    /**
     * Résolution d'un code tiers — sert à suivre le pointeur de doublon
     * {@code ComProspection.TempField10}, qui porte un {@code CfgTier.Code} (849 valeurs réelles,
     * 849 résolues).
     *
     * <p><b>Rend une {@code List}, jamais un {@code Optional} : {@code Code} n'est pas unique.</b>
     * 14 516 tiers pour 14 504 codes distincts — {@code "000009"} est porté par 4 tiers. Aucun des
     * 849 pointeurs réels n'est ambigu aujourd'hui, mais un dérivé {@code Optional} lèverait
     * {@code IncorrectResultSizeDataAccessException} le jour où l'un d'eux tombe sur un doublon de
     * code. L'appelant décide quoi faire de la multiplicité — voir
     * {@code instructions/FEATURE-marqueurs-tri-pre-prospection.md} §3.
     */
    List<CfgTier> findByCode(String code);
}
