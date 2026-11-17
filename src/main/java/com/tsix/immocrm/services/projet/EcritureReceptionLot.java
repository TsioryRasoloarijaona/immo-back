package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.ReceptionDetailDto;
import com.tsix.immocrm.dto.projet.ReceptionSaisieDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EcritureReceptionLot {

    public ReceptionDetailDto lire(UUID lot, UUID reception) {
        throw new UnsupportedOperationException();
    }

    public ReceptionDetailDto creer(UUID lot, ReceptionSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public ReceptionDetailDto modifier(UUID lot, UUID reception, ReceptionSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID lot, UUID reception) {
        throw new UnsupportedOperationException();
    }
}
