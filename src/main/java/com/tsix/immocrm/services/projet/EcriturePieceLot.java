package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.NaturePieceLot;
import com.tsix.immocrm.dto.projet.PieceLotDetailDto;
import com.tsix.immocrm.dto.projet.PieceLotModeleDto;
import com.tsix.immocrm.dto.projet.PieceLotSaisieDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Pièces à payer créées depuis la fiche lot (FEATURE-lot-pieces-a-payer.md) ; ordre des refus : 404, 403, 400, 409. */
@Service
public class EcriturePieceLot {

    public PieceLotModeleDto modele(UUID lot, NaturePieceLot nature) {
        throw new UnsupportedOperationException();
    }

    public PieceLotDetailDto creer(UUID lot, PieceLotSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public PieceLotDetailDto lire(UUID lot, UUID piece) {
        throw new UnsupportedOperationException();
    }

    public PieceLotDetailDto modifier(UUID lot, UUID piece, PieceLotSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID lot, UUID piece) {
        throw new UnsupportedOperationException();
    }

    public void supprimerAvoir(UUID lot, UUID avoir) {
        throw new UnsupportedOperationException();
    }
}
