package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.faisabilite.DemandeTransitionEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.EtudeFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.HistoriqueStatutEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TransitionPossibleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TransitionFaisabiliteService {

    public List<TransitionPossibleDto> lister(UUID etudeId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public EtudeFaisabiliteDto modifierStatut(UUID etudeId, DemandeTransitionEtudeDto demande) {
        throw new UnsupportedOperationException();
    }

    public List<HistoriqueStatutEtudeDto> historique(UUID etudeId) {
        throw new UnsupportedOperationException();
    }
}
