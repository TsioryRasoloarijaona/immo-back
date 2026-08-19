package com.tsix.immocrm.services.prospect;

import com.tsix.immocrm.dto.prospect.CommercialListItemDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CommercialService {

    // Confirmed with the user: commercial staff assignable to a Dossier/Prospection.
    private static final Set<String> COMMERCIAL_KIND_CODES = Set.of("COM", "AGC", "ATC", "ACC", "RC");

    private final CfgTierRepository tierRepository;

    public CommercialService(CfgTierRepository tierRepository) {
        this.tierRepository = tierRepository;
    }

    public Page<CommercialListItemDto> findAll(Pageable pageable) {
        return tierRepository.findAll((root, query, cb) ->
                        root.get("cfgTierKind").get("code").in(COMMERCIAL_KIND_CODES), pageable)
                .map(CommercialService::toListItem);
    }

    private static CommercialListItemDto toListItem(CfgTier tier) {
        return new CommercialListItemDto(
                tier.getId(),
                tier.getCode(),
                tier.getFirstName(),
                tier.getLastName(),
                tier.getEMail(),
                tier.getPhone(),
                tier.getCfgTierKind() != null ? tier.getCfgTierKind().getCode() : null
        );
    }
}
