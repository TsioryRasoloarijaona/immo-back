package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.dto.achat.demandes.DemandeAchatFiche;
import com.tsix.immocrm.dto.achat.demandes.DemandeAchatLigne;
import com.tsix.immocrm.dto.achat.demandes.ExecutionTransitionDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.FiltresDemandesAchat;
import com.tsix.immocrm.dto.achat.demandes.HistoriqueDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.SaisieDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.TransitionDemandeAchat;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DemandesAchatService {

    public List<DemandeAchatLigne> liste(FiltresDemandesAchat filtres) {
        throw new UnsupportedOperationException();
    }

    public List<DemandeAchatLigne> export(FiltresDemandesAchat filtres) {
        throw new UnsupportedOperationException();
    }

    public DemandeAchatFiche fiche(UUID id) {
        throw new UnsupportedOperationException();
    }

    public DemandeAchatFiche creer(SaisieDemandeAchat saisie) {
        throw new UnsupportedOperationException();
    }

    public DemandeAchatFiche modifier(UUID id, SaisieDemandeAchat saisie) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<TransitionDemandeAchat> transitions(UUID id) {
        throw new UnsupportedOperationException();
    }

    public DemandeAchatFiche executer(UUID id, ExecutionTransitionDemandeAchat execution) {
        throw new UnsupportedOperationException();
    }

    public List<HistoriqueDemandeAchat> historique(UUID id) {
        throw new UnsupportedOperationException();
    }
}
