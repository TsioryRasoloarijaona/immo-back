package com.tsix.immocrm.services.projet.reunions;

import com.tsix.immocrm.dto.projet.reunions.AvancementSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.ContactSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.DecisionSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.IntervenantSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.MaitresOuvrageAjoutDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionFicheDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OngletsReunionService {

    public ReunionFicheDto ajouterMaitresOuvrage(UUID id, MaitresOuvrageAjoutDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto modifierMaitreOuvrage(UUID id, UUID ligneId, ContactSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto supprimerMaitreOuvrage(UUID id, UUID ligneId) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto creerIntervenant(UUID id, IntervenantSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto modifierIntervenant(UUID id, UUID ligneId, IntervenantSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto supprimerIntervenant(UUID id, UUID ligneId) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto creerContactIntervenant(UUID id, UUID intervenantId, ContactSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto modifierContactIntervenant(UUID id, UUID intervenantId, UUID ligneId, ContactSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto supprimerContactIntervenant(UUID id, UUID intervenantId, UUID ligneId) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto creerDecision(UUID id, DecisionSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto modifierDecision(UUID id, UUID ligneId, DecisionSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto supprimerDecision(UUID id, UUID ligneId) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto modifierAvancement(UUID id, AvancementSaisieDto corps) {
        throw new UnsupportedOperationException();
    }
}
