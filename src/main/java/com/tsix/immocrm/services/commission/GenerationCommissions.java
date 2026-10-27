package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.domain.commission.TypeCommission;
import com.tsix.immocrm.dto.commission.ResultatGenerationDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GenerationCommissions {

    public ResultatGenerationDto generer(UUID trancheId, TypeCommission type, List<UUID> dossierIds) {
        throw new UnsupportedOperationException();
    }
}
