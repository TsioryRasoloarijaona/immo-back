package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.PrevisionsLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsSaisieLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsSaisieRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Saisie et enregistrement des prévisions d'un lot (FEATURE-lot-previsions.md §4.2-4.3). */
@Service
public class PrevisionsLotService {

    public PrevisionsSaisieLotDto saisie(UUID id) {
        throw new UnsupportedOperationException(id.toString());
    }

    public PrevisionsLotDto enregistrer(UUID id, PrevisionsSaisieRequest corps) {
        throw new UnsupportedOperationException(id + " " + corps);
    }
}
