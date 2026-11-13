package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.BordereauDto;
import com.tsix.immocrm.dto.projet.BordereauEstimatifDto;
import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Enregistrement d'un bordereau entier (FEATURE-lot-ecritures.md §3). */
@Service
public class EcritureBordereauLot {

    public BordereauEstimatifDto ecrireEstimatif(UUID id, BordereauSaisieDto corps) {
        throw new UnsupportedOperationException();
    }

    public BordereauDto ecrireDefinitif(UUID id, BordereauSaisieDto corps) {
        throw new UnsupportedOperationException();
    }
}
