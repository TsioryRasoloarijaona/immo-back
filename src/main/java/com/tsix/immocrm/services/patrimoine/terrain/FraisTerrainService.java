package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.dto.patrimoine.LigneFraisTerrainDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FraisTerrainService {

    public LignesTrancheDto<LigneFraisTerrainDto> lire(UUID terrainId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public LignesTrancheDto<LigneFraisTerrainDto> enregistrer(UUID terrainId, LignesTrancheDto<LigneFraisTerrainDto> corps) {
        throw new UnsupportedOperationException();
    }
}
