package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.dto.patrimoine.prospectionterrain.TerrainCreeDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.TransformationTerrainDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** « Transformer en terrain » (FEATURE-patrimoine-prospections-terrain.md §5.7). */
@Service
public class TransformationProspectionTerrain {

    @Transactional
    public TerrainCreeDto transformer(UUID prospectionId, TransformationTerrainDto saisie) {
        throw new UnsupportedOperationException();
    }
}
