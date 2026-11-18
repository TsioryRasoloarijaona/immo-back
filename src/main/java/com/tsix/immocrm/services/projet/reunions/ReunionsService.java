package com.tsix.immocrm.services.projet.reunions;

import com.tsix.immocrm.dto.projet.reunions.ChoixReunionsDto;
import com.tsix.immocrm.dto.projet.reunions.FiltresReunions;
import com.tsix.immocrm.dto.projet.reunions.ReunionCreationDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionFicheDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionLigneDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionModificationDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReunionsService {

    public List<ReunionLigneDto> liste(FiltresReunions filtres) {
        throw new UnsupportedOperationException();
    }

    public ChoixReunionsDto choix(UUID projetId) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto fiche(UUID id) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto creer(ReunionCreationDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReunionFicheDto modifier(UUID id, ReunionModificationDto corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }
}
