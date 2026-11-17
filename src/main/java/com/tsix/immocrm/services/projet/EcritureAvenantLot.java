package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.AvenantDetailDto;
import com.tsix.immocrm.dto.projet.AvenantSaisieDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EcritureAvenantLot {

    public AvenantDetailDto lire(UUID lot, UUID avenant) {
        throw new UnsupportedOperationException();
    }

    public AvenantDetailDto creer(UUID lot, AvenantSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public AvenantDetailDto modifier(UUID lot, UUID avenant, AvenantSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID lot, UUID avenant) {
        throw new UnsupportedOperationException();
    }
}
