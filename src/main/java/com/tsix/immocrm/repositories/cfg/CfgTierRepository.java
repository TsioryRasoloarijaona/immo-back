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
}
