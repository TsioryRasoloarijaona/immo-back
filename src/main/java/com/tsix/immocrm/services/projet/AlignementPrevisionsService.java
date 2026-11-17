package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.AlignementPrevisionsDto;
import com.tsix.immocrm.dto.projet.AlignementPrevisionsRequest;
import com.tsix.immocrm.dto.projet.ModeleSimulationSaisieDto;
import com.tsix.immocrm.dto.projet.PropositionPointsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Propositions de points « Prévisions » d'un lot, sans écriture (FEATURE-lot-previsions-reel.md §4 A1-A6, M5, M6). */
@Service
@Transactional(readOnly = true)
public class AlignementPrevisionsService {

    public AlignementPrevisionsDto ecarts(UUID id) {
        throw new UnsupportedOperationException();
    }

    public PropositionPointsDto aligner(UUID id, AlignementPrevisionsRequest corps) {
        throw new UnsupportedOperationException();
    }

    public PropositionPointsDto appliquerModele(UUID id, UUID modeleId) {
        throw new UnsupportedOperationException();
    }

    public ModeleSimulationSaisieDto modelePropose(UUID id) {
        throw new UnsupportedOperationException();
    }
}
