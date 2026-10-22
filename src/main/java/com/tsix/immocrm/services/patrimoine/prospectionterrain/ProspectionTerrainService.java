package com.tsix.immocrm.services.patrimoine.prospectionterrain;

import com.tsix.immocrm.domain.foncier.SurfacesTerrain;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ApercuCalculDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ExecutionTransitionDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.FiltresProspectionsTerrain;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.HistoriqueStatutProspectionDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainDetailDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainListeDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.TransitionProspectionTerrainDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ZoneTerrainDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ZonesTerrainEcritureDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Prospections terrain (FEATURE-patrimoine-prospections-terrain.md §5). */
@Service
@Transactional(readOnly = true)
public class ProspectionTerrainService {

    public Page<ProspectionTerrainListeDto> lister(FiltresProspectionsTerrain filtres, Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    public ProspectionTerrainDetailDto lire(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public ProspectionTerrainDetailDto creer(ProspectionTerrainEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public ProspectionTerrainDetailDto modifier(UUID id, ProspectionTerrainEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public List<ZoneTerrainDto> modifierZones(UUID id, ZonesTerrainEcritureDto saisie) {
        throw new UnsupportedOperationException();
    }

    public SurfacesTerrain apercu(ApercuCalculDto saisie) {
        throw new UnsupportedOperationException();
    }

    public List<TransitionProspectionTerrainDto> transitions(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public ProspectionTerrainDetailDto executerTransition(UUID id, ExecutionTransitionDto saisie) {
        throw new UnsupportedOperationException();
    }

    public List<HistoriqueStatutProspectionDto> historique(UUID id) {
        throw new UnsupportedOperationException();
    }
}
