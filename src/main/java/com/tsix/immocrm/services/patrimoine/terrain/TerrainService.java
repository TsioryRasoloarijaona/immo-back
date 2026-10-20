package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.dto.patrimoine.FiltresTerrains;
import com.tsix.immocrm.dto.patrimoine.TerrainDetailDto;
import com.tsix.immocrm.dto.patrimoine.TerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TerrainListeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TerrainService {

    public Page<TerrainListeDto> lister(FiltresTerrains filtres, boolean lotissements, Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    public TerrainDetailDto lire(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public TerrainDetailDto creer(TerrainEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public TerrainDetailDto modifier(UUID id, TerrainEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public TerrainDetailDto creerSousTerrain(UUID parentId, TerrainEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }
}
