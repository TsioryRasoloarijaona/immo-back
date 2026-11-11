package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.CreerDecompteRequest;
import com.tsix.immocrm.dto.projet.DecompteDetailDto;
import com.tsix.immocrm.dto.projet.EnregistrerDecompteRequest;
import com.tsix.immocrm.dto.projet.LigneDecompteDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ActionsDecompteService {

    public DecompteDetailDto creer(UUID lotId, CreerDecompteRequest demande) {
        throw new UnsupportedOperationException();
    }

    public DecompteDetailDto enregistrer(UUID id, EnregistrerDecompteRequest demande) {
        throw new UnsupportedOperationException();
    }

    public DecompteDetailDto definirDernier(UUID id) {
        throw new UnsupportedOperationException();
    }

    public DecompteDetailDto recalculer(UUID id) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<LigneDecompteDto> lignesRechargees(UUID id) {
        throw new UnsupportedOperationException();
    }
}
