package com.tsix.immocrm.services.desistement;

import com.tsix.immocrm.dto.desistement.CreerDesistementRequest;
import com.tsix.immocrm.dto.desistement.DesistementDto;
import com.tsix.immocrm.dto.desistement.DesistementsPageDto;
import com.tsix.immocrm.dto.desistement.FiltresDesistements;
import com.tsix.immocrm.dto.desistement.ModifierDesistementRequest;
import com.tsix.immocrm.dto.desistement.SimulationDesistementDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DesistementService {

    public DesistementsPageDto lister(FiltresDesistements filtres, Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    public DesistementDto detail(UUID id) {
        throw new UnsupportedOperationException();
    }

    public DesistementDto duDossier(UUID dossierId) {
        throw new UnsupportedOperationException();
    }

    public SimulationDesistementDto simuler(UUID dossierId) {
        throw new UnsupportedOperationException();
    }

    public DesistementDto creer(UUID dossierId, CreerDesistementRequest request) {
        throw new UnsupportedOperationException();
    }

    public DesistementDto modifier(UUID id, ModifierDesistementRequest request) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<ReferentielItemDto> motifs() {
        throw new UnsupportedOperationException();
    }
}
