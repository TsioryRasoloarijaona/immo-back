package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.faisabilite.StatutEtudeConfigDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TransitionEtudeConfigDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConfigWorkflowFaisabiliteService {

    public List<StatutEtudeConfigDto> listerStatuts() {
        throw new UnsupportedOperationException();
    }

    public StatutEtudeConfigDto statut(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public StatutEtudeConfigDto creerStatut(StatutEtudeConfigDto saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public StatutEtudeConfigDto modifierStatut(UUID id, StatutEtudeConfigDto saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void supprimerStatut(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<TransitionEtudeConfigDto> listerTransitions() {
        throw new UnsupportedOperationException();
    }

    public TransitionEtudeConfigDto transition(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public TransitionEtudeConfigDto creerTransition(TransitionEtudeConfigDto saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public TransitionEtudeConfigDto modifierTransition(UUID id, TransitionEtudeConfigDto saisie) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void supprimerTransition(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<ReferentielItemDto> typesEtat() {
        throw new UnsupportedOperationException();
    }
}
