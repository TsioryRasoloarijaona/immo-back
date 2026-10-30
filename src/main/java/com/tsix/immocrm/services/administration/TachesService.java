package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.ChangerStatutTacheRequest;
import com.tsix.immocrm.dto.administration.FiltresTaches;
import com.tsix.immocrm.dto.administration.StatutTacheDto;
import com.tsix.immocrm.dto.administration.TacheDto;
import com.tsix.immocrm.dto.administration.TacheEcritureDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TachesService {

    public PageDto<TacheDto> lister(FiltresTaches filtres, Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    public List<StatutTacheDto> statuts() {
        throw new UnsupportedOperationException();
    }

    public List<ReferentielItemDto> modules() {
        throw new UnsupportedOperationException();
    }

    public TacheDto detail(UUID id) {
        throw new UnsupportedOperationException();
    }

    public TacheDto creer(TacheEcritureDto corps) {
        throw new UnsupportedOperationException();
    }

    public TacheDto modifier(UUID id, TacheEcritureDto corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }

    public TacheDto changerStatut(UUID id, ChangerStatutTacheRequest corps) {
        throw new UnsupportedOperationException();
    }
}
