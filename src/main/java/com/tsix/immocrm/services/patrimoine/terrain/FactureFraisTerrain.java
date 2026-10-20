package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.dto.patrimoine.FactureFraisTerrainDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class FactureFraisTerrain {

    @Transactional
    public FactureFraisTerrainDto valider(UUID terrainId, UUID fraisId) {
        throw new UnsupportedOperationException();
    }
}
