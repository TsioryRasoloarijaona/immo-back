package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.AvancesLotDto;
import com.tsix.immocrm.dto.projet.AvenantLotDto;
import com.tsix.immocrm.dto.projet.AvoirLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsLotDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Sous-fenêtres de la fiche lot en lecture (FEATURE-lot-sous-fenetres.md) ; chaque route lit d'abord le lot visible. */
@Service
@Transactional(readOnly = true)
public class SousFenetresLotService {

    public AvancesLotDto avances(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<AvoirLotDto> avoirs(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<AvenantLotDto> avenants(UUID id) {
        throw new UnsupportedOperationException();
    }

    public RemboursementsLotDto remboursements(UUID id) {
        throw new UnsupportedOperationException();
    }

    public PrevisionsLotDto previsions(UUID id) {
        throw new UnsupportedOperationException();
    }
}
