package com.tsix.immocrm.services.tresorerie;

import com.tsix.immocrm.dto.tresorerie.DecaissementFicheDto;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie.ModuleTresorerie;
import com.tsix.immocrm.dto.tresorerie.PieceAAffecterDto;
import com.tsix.immocrm.dto.tresorerie.ReferentielsDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.SaisieDecaissementDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SaisieDecaissementService {

    public DecaissementFicheDto creer(SaisieDecaissementDto saisie) {
        throw new UnsupportedOperationException();
    }

    public DecaissementFicheDto modifier(UUID id, SaisieDecaissementDto saisie) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }

    public ReferentielsDecaissementDto referentiels(ModuleTresorerie module) {
        throw new UnsupportedOperationException();
    }

    public List<PieceAAffecterDto> piecesAAffecter(ModuleTresorerie module, UUID fournisseurId, UUID societeId,
                                                   UUID reglementId) {
        throw new UnsupportedOperationException();
    }
}
