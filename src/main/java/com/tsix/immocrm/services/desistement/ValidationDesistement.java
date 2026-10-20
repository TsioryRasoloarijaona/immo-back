package com.tsix.immocrm.services.desistement;

import com.tsix.immocrm.dto.desistement.DesistementDto;
import com.tsix.immocrm.dto.desistement.RembourserDesistementRequest;
import com.tsix.immocrm.dto.desistement.ValiderDesistementRequest;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ValidationDesistement {

    public DesistementDto valider(UUID id, ValiderDesistementRequest request) {
        throw new UnsupportedOperationException();
    }

    public DesistementDto rembourser(UUID id, RembourserDesistementRequest request, AuthenticatedUser principal) {
        throw new UnsupportedOperationException();
    }
}
