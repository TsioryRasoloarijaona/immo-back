package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** « Charger à partir d'une autre tranche » (FEATURE-patrimoine-reliquat-v2.md §A). */
@Service
public class ChargementTrancheService {

    @Transactional
    public TrancheDetailDto charger(UUID cibleId, UUID sourceId) {
        throw new UnsupportedOperationException();
    }
}
