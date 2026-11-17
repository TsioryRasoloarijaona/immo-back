package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.ModeleSimulationDetailDto;
import com.tsix.immocrm.dto.projet.ModeleSimulationListItemDto;
import com.tsix.immocrm.dto.projet.ModeleSimulationSaisieDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Modèles de simulation lot {@code PrjMarketPrevisionModel(Line)} (FEATURE-lot-previsions-reel.md §4 M1-M4, M7). */
@Service
@Transactional
public class ModelesSimulationService {

    @Transactional(readOnly = true)
    public List<ModeleSimulationListItemDto> liste(UUID categorieId) {
        throw new UnsupportedOperationException();
    }

    @Transactional(readOnly = true)
    public ModeleSimulationDetailDto detail(UUID id) {
        throw new UnsupportedOperationException();
    }

    public ModeleSimulationDetailDto creer(ModeleSimulationSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public ModeleSimulationDetailDto modifier(UUID id, ModeleSimulationSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }
}
