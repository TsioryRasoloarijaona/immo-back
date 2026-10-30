package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.FiltresRendezVous;
import com.tsix.immocrm.dto.administration.RendezVousDto;
import com.tsix.immocrm.dto.administration.RendezVousEcritureDto;
import com.tsix.immocrm.dto.common.PageDto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RendezVousService {

    public PageDto<RendezVousDto> lister(FiltresRendezVous filtres, Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    public List<RendezVousDto> calendrier(LocalDate debut, LocalDate fin, UUID gestionnaireId) {
        throw new UnsupportedOperationException();
    }

    public List<Map<String, Object>> lignesExport(FiltresRendezVous filtres) {
        throw new UnsupportedOperationException();
    }

    public RendezVousDto detail(UUID id) {
        throw new UnsupportedOperationException();
    }

    public RendezVousDto creer(RendezVousEcritureDto corps) {
        throw new UnsupportedOperationException();
    }

    public RendezVousDto modifier(UUID id, RendezVousEcritureDto corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }
}
